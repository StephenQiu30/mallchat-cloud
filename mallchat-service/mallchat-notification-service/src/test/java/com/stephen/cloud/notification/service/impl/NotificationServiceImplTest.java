package com.stephen.cloud.notification.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.notification.model.enums.NotificationStatusEnum;
import com.stephen.cloud.api.notification.model.enums.NotificationTypeEnum;
import com.stephen.cloud.api.notification.model.vo.NotificationVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.notification.model.entity.Notification;
import com.stephen.cloud.notification.mq.NotificationMqProducer;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * NotificationServiceImpl 核心行为测试
 * <p>
 * 覆盖通知列表、未读数、单条已读、批量已读和实时推送的核心行为。
 * 使用 Testable 子类 + Proxy 模式，与项目既有测试风格保持一致。
 * </p>
 */
class NotificationServiceImplTest {

    private TestableNotificationServiceImpl service;
    private List<Notification> savedNotifications;
    private List<Notification> updatedNotifications;
    private List<NotificationVO> sentMessages;
    private Page<Notification> pageResult;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                Notification.class);
        service = new TestableNotificationServiceImpl();
        savedNotifications = new ArrayList<>();
        updatedNotifications = new ArrayList<>();
        sentMessages = new ArrayList<>();
        pageResult = new Page<>(1, 20, 0);

        ReflectionTestUtils.setField(service, "notificationMqProducer",
                new FakeNotificationMqProducer());
        ReflectionTestUtils.setField(service, "userFeignClient", createFakeUserFeignClient());
    }

    // ========== 通知列表 + 用户信息装配 ==========

    @Test
    void shouldReturnNotificationVOPageEnrichedWithUserInfo() {
        Notification n1 = buildNotification(100L, 1L, NotificationStatusEnum.UNREAD.getValue());
        Notification n2 = buildNotification(101L, 1L, NotificationStatusEnum.READ.getValue());
        pageResult.setRecords(List.of(n1, n2));
        pageResult.setTotal(2);

        Page<NotificationVO> result = service.getNotificationVOPage(pageResult);

        Assertions.assertEquals(2, result.getRecords().size());
        NotificationVO vo1 = result.getRecords().get(0);
        Assertions.assertEquals(100L, vo1.getId());
        Assertions.assertEquals(1L, vo1.getUserId());
        Assertions.assertNotNull(vo1.getUserVO());
        Assertions.assertEquals(1L, vo1.getUserVO().getId());
        Assertions.assertEquals("user1", vo1.getUserVO().getUserName());
    }

    @Test
    void shouldHandleEmptyPageGracefully() {
        pageResult.setRecords(List.of());
        pageResult.setTotal(0);

        Page<NotificationVO> result = service.getNotificationVOPage(pageResult);

        Assertions.assertEquals(0, result.getRecords().size());
        Assertions.assertEquals(0, result.getTotal());
    }

    // ========== 未读数统计 ==========

    @Test
    void shouldReturnCorrectUnreadCount() {
        savedNotifications.add(buildNotification(200L, 1L, NotificationStatusEnum.UNREAD.getValue()));
        savedNotifications.add(buildNotification(201L, 1L, NotificationStatusEnum.UNREAD.getValue()));
        savedNotifications.add(buildNotification(202L, 1L, NotificationStatusEnum.READ.getValue()));

        long count = service.getUnreadCount(1L);

        Assertions.assertEquals(2, count);
    }

    @Test
    void shouldReturnZeroWhenNoUnreadNotifications() {
        savedNotifications.add(buildNotification(200L, 1L, NotificationStatusEnum.READ.getValue()));

        long count = service.getUnreadCount(1L);

        Assertions.assertEquals(0, count);
    }

    // ========== 单条已读 ==========

    @Test
    void shouldMarkSingleNotificationAsRead() {
        Notification notification = buildNotification(300L, 1L, NotificationStatusEnum.UNREAD.getValue());
        savedNotifications.add(notification);

        boolean result = service.markRead(300L, 1L, false);

        Assertions.assertTrue(result);
        Assertions.assertEquals(NotificationStatusEnum.READ.getValue(), notification.getIsRead());
    }

    @Test
    void shouldAllowAdminToMarkOtherUsersNotificationAsRead() {
        Notification notification = buildNotification(301L, 2L, NotificationStatusEnum.UNREAD.getValue());
        savedNotifications.add(notification);

        boolean result = service.markRead(301L, 999L, true);

        Assertions.assertTrue(result);
        Assertions.assertEquals(NotificationStatusEnum.READ.getValue(), notification.getIsRead());
    }

    @Test
    void shouldRejectNonOwnerNonAdminMarkRead() {
        Notification notification = buildNotification(302L, 1L, NotificationStatusEnum.UNREAD.getValue());
        savedNotifications.add(notification);

        Assertions.assertThrows(BusinessException.class,
                () -> service.markRead(302L, 999L, false));
    }

    @Test
    void shouldReturnTrueWhenMarkingAlreadyReadNotificationIdempotent() {
        Notification notification = buildNotification(303L, 1L, NotificationStatusEnum.READ.getValue());
        savedNotifications.add(notification);

        boolean result = service.markRead(303L, 1L, false);

        Assertions.assertTrue(result);
    }

    // ========== 批量已读 ==========

    @Test
    void shouldBatchMarkMultipleUnreadNotificationsAsRead() {
        Notification n1 = buildNotification(400L, 1L, NotificationStatusEnum.UNREAD.getValue());
        Notification n2 = buildNotification(401L, 1L, NotificationStatusEnum.UNREAD.getValue());
        savedNotifications.add(n1);
        savedNotifications.add(n2);

        int updated = service.batchMarkRead(List.of(400L, 401L), 1L, false);

        Assertions.assertEquals(2, updated);
        Assertions.assertEquals(NotificationStatusEnum.READ.getValue(), n1.getIsRead());
        Assertions.assertEquals(NotificationStatusEnum.READ.getValue(), n2.getIsRead());
    }

    @Test
    void shouldSkipAlreadyReadNotificationsInBatchMarkRead() {
        Notification unread = buildNotification(402L, 1L, NotificationStatusEnum.UNREAD.getValue());
        Notification alreadyRead = buildNotification(403L, 1L, NotificationStatusEnum.READ.getValue());
        savedNotifications.add(unread);
        savedNotifications.add(alreadyRead);

        int updated = service.batchMarkRead(List.of(402L, 403L), 1L, false);

        Assertions.assertEquals(1, updated);
        Assertions.assertEquals(NotificationStatusEnum.READ.getValue(), unread.getIsRead());
        Assertions.assertEquals(NotificationStatusEnum.READ.getValue(), alreadyRead.getIsRead());
    }

    @Test
    void shouldReturnZeroWhenAllNotificationsAlreadyReadIdempotent() {
        Notification n1 = buildNotification(404L, 1L, NotificationStatusEnum.READ.getValue());
        Notification n2 = buildNotification(405L, 1L, NotificationStatusEnum.READ.getValue());
        savedNotifications.add(n1);
        savedNotifications.add(n2);

        int updated = service.batchMarkRead(List.of(404L, 405L), 1L, false);

        Assertions.assertEquals(0, updated);
    }

    @Test
    void shouldRejectBatchMarkReadForOtherUsersNotifications() {
        Notification otherUser = buildNotification(406L, 2L, NotificationStatusEnum.UNREAD.getValue());
        savedNotifications.add(otherUser);

        int updated = service.batchMarkRead(List.of(406L), 1L, false);

        Assertions.assertEquals(0, updated);
        Assertions.assertEquals(NotificationStatusEnum.UNREAD.getValue(), otherUser.getIsRead());
    }

    @Test
    void shouldAllowAdminToBatchMarkReadOtherUsersNotifications() {
        Notification otherUser = buildNotification(407L, 2L, NotificationStatusEnum.UNREAD.getValue());
        savedNotifications.add(otherUser);

        int updated = service.batchMarkRead(List.of(407L), 999L, true);

        Assertions.assertEquals(1, updated);
        Assertions.assertEquals(NotificationStatusEnum.READ.getValue(), otherUser.getIsRead());
    }

    @Test
    void shouldReturnZeroForEmptyBatchMarkReadIds() {
        int updated = service.batchMarkRead(List.of(), 1L, false);
        Assertions.assertEquals(0, updated);
    }

    // ========== 端到端验收：查询+批量已读后状态和未读数正确 ==========

    @Test
    void endToEndShouldQueryBatchReadAndVerifyUnreadCountUpdated() {
        // 用户 100L 有 3 条未读通知
        Notification n1 = buildNotification(600L, 100L, NotificationStatusEnum.UNREAD.getValue());
        Notification n2 = buildNotification(601L, 100L, NotificationStatusEnum.UNREAD.getValue());
        Notification n3 = buildNotification(602L, 100L, NotificationStatusEnum.UNREAD.getValue());
        savedNotifications.addAll(List.of(n1, n2, n3));

        // 初始未读数
        Assertions.assertEquals(3, service.getUnreadCount(100L));

        // 批量已读其中 2 条
        int updated = service.batchMarkRead(List.of(600L, 601L), 100L, false);
        Assertions.assertEquals(2, updated);

        // 未读数更新为 1
        Assertions.assertEquals(1, service.getUnreadCount(100L));

        // 重复批量已读已读通知，幂等返回 0
        int repeatUpdated = service.batchMarkRead(List.of(600L, 601L), 100L, false);
        Assertions.assertEquals(0, repeatUpdated);

        // 未读数不变
        Assertions.assertEquals(1, service.getUnreadCount(100L));
    }

    // ========== 通知创建 + MQ 推送 ==========

    @Test
    void shouldCreateNotificationAndTriggerMqPush() {
        Notification notification = new Notification();
        notification.setTitle("新通知");
        notification.setContent("你有一条新消息");
        notification.setType(NotificationTypeEnum.SYSTEM.getCode());
        notification.setUserId(1L);

        Long id = service.addNotification(notification);

        Assertions.assertNotNull(id);
        Assertions.assertEquals(1, savedNotifications.size());
        Assertions.assertEquals(1, sentMessages.size());
        Assertions.assertEquals(id, sentMessages.get(0).getId());
        Assertions.assertEquals(1L, sentMessages.get(0).getUserId());
        Assertions.assertEquals("新通知", sentMessages.get(0).getTitle());
    }

    @Test
    void shouldNotCreateDuplicateNotificationWithSameBizIdAndUserId() {
        Notification existing = new Notification();
        existing.setId(500L);
        existing.setBizId("biz_001");
        existing.setUserId(1L);
        existing.setTitle("已有");
        existing.setContent("已存在");
        existing.setType(NotificationTypeEnum.SYSTEM.getCode());
        existing.setIsRead(NotificationStatusEnum.UNREAD.getValue());
        savedNotifications.add(existing);

        Notification duplicate = new Notification();
        duplicate.setBizId("biz_001");
        duplicate.setUserId(1L);
        duplicate.setTitle("重复");
        duplicate.setContent("重复内容");
        duplicate.setType(NotificationTypeEnum.SYSTEM.getCode());

        Long id = service.addNotification(duplicate);

        Assertions.assertEquals(500L, id);
        Assertions.assertEquals(1, savedNotifications.size());
    }

    @Test
    void shouldDefaultNewNotificationToUnread() {
        Notification notification = new Notification();
        notification.setTitle("测试");
        notification.setContent("内容");
        notification.setType(NotificationTypeEnum.SYSTEM.getCode());
        notification.setUserId(1L);

        service.addNotification(notification);

        Assertions.assertEquals(NotificationStatusEnum.UNREAD.getValue(),
                savedNotifications.get(0).getIsRead());
    }

    // ========== 辅助方法 ==========

    private Notification buildNotification(Long id, Long userId, Integer isRead) {
        Notification n = new Notification();
        n.setId(id);
        n.setTitle("通知" + id);
        n.setContent("内容" + id);
        n.setType(NotificationTypeEnum.SYSTEM.getCode());
        n.setUserId(userId);
        n.setIsRead(isRead);
        n.setStatus(0);
        n.setBizId("biz_" + id);
        return n;
    }

    // ========== Testable 子类 ==========

    private class TestableNotificationServiceImpl extends NotificationServiceImpl {
        @Override
        public <E extends IPage<Notification>> E page(E page, Wrapper<Notification> queryWrapper) {
            @SuppressWarnings("unchecked")
            E result = (E) pageResult;
            return result;
        }

        @Override
        public Notification getById(java.io.Serializable id) {
            return savedNotifications.stream()
                    .filter(n -> n.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public boolean save(Notification entity) {
            entity.setId((long) (savedNotifications.size() + 1));
            savedNotifications.add(entity);
            return true;
        }

        @Override
        public boolean updateById(Notification entity) {
            updatedNotifications.add(entity);
            for (int i = 0; i < savedNotifications.size(); i++) {
                if (savedNotifications.get(i).getId().equals(entity.getId())) {
                    savedNotifications.set(i, entity);
                    return true;
                }
            }
            return true;
        }

        @Override
        public List<Notification> listByIds(Collection<? extends java.io.Serializable> idList) {
            return savedNotifications.stream()
                    .filter(n -> idList.contains(n.getId()))
                    .collect(Collectors.toList());
        }

        @Override
        public boolean updateBatchById(Collection<Notification> entityList) {
            updatedNotifications.addAll(entityList);
            for (Notification entity : entityList) {
                for (int i = 0; i < savedNotifications.size(); i++) {
                    if (savedNotifications.get(i).getId().equals(entity.getId())) {
                        savedNotifications.set(i, entity);
                        break;
                    }
                }
            }
            return true;
        }

        @Override
        public long getUnreadCount(Long userId) {
            return savedNotifications.stream()
                    .filter(n -> n.getUserId().equals(userId))
                    .filter(n -> NotificationStatusEnum.UNREAD.getValue().equals(n.getIsRead()))
                    .count();
        }

        @Override
        public boolean markAllRead(Long userId) {
            List<Notification> unread = savedNotifications.stream()
                    .filter(n -> n.getUserId().equals(userId))
                    .filter(n -> NotificationStatusEnum.UNREAD.getValue().equals(n.getIsRead()))
                    .collect(Collectors.toList());
            if (unread.isEmpty()) {
                return true;
            }
            unread.forEach(n -> n.setIsRead(NotificationStatusEnum.READ.getValue()));
            return updateBatchById(unread);
        }

        @Override
        public Long addNotification(Notification notification) {
            validNotification(notification, true);

            String rawBizId = notification.getBizId();
            final String bizId = org.apache.commons.lang3.StringUtils.isBlank(rawBizId)
                    ? "manual_" + cn.hutool.core.util.IdUtil.fastSimpleUUID()
                    : rawBizId;
            notification.setBizId(bizId);

            Long userId = notification.getUserId();
            Notification existing = savedNotifications.stream()
                    .filter(n -> bizId.equals(n.getBizId()) && userId.equals(n.getUserId()))
                    .findFirst()
                    .orElse(null);
            if (existing != null) {
                return existing.getId();
            }

            if (notification.getIsRead() == null) {
                notification.setIsRead(NotificationStatusEnum.UNREAD.getValue());
            }

            save(notification);
            sendPushNotification(notification);
            return notification.getId();
        }

        private void sendPushNotification(Notification notification) {
            NotificationVO vo = com.stephen.cloud.notification.convert.NotificationConvert.objToVo(notification);
            sentMessages.add(vo);
        }
    }

    // ========== Fake 组件 ==========

    private class FakeNotificationMqProducer extends NotificationMqProducer {
        @Override
        public void sendNotificationCreated(NotificationVO notificationVO) {
            sentMessages.add(notificationVO);
        }
    }

    @SuppressWarnings("unchecked")
    private UserFeignClient createFakeUserFeignClient() {
        return (UserFeignClient) Proxy.newProxyInstance(
                UserFeignClient.class.getClassLoader(),
                new Class[]{UserFeignClient.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUserVOById" -> fakeUserResponse((Long) args[0]);
                    case "getUserVOByIds" -> fakeUserListResponse((List<Long>) args[0]);
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private BaseResponse<UserVO> fakeUserResponse(Long userId) {
        UserVO user = new UserVO();
        user.setId(userId);
        user.setUserName("user" + userId);
        return new BaseResponse<>(0, user, "ok");
    }

    private BaseResponse<List<UserVO>> fakeUserListResponse(List<Long> userIds) {
        List<UserVO> users = userIds.stream().map(id -> {
            UserVO u = new UserVO();
            u.setId(id);
            u.setUserName("user" + id);
            return u;
        }).collect(Collectors.toList());
        return new BaseResponse<>(0, users, "ok");
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        return null;
    }
}
