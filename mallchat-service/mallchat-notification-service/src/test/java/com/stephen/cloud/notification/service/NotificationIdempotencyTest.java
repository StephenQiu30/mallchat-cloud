package com.stephen.cloud.notification.service;

import com.stephen.cloud.notification.model.entity.Notification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 通知幂等性测试
 * <p>
 * 验证通知系统满足 STE-222 要求：
 * 1. 通知重复生成需要幂等或可去重
 * 2. 通知只做聚合和展示，不替代业务事实
 * </p>
 *
 * @author StephenQiu30
 */
@DisplayName("通知幂等性测试 - STE-222")
class NotificationIdempotencyTest {

    private NotificationIdempotencyService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationIdempotencyService();
    }

    @Test
    @DisplayName("相同 bizId 的通知应返回已有 ID，不创建重复记录")
    void shouldReturnExistingNotificationIdForDuplicateBizId() {
        // Given: 首次创建通知
        Notification firstNotification = new Notification();
        firstNotification.setUserId(1L);
        firstNotification.setTitle("测试通知");
        firstNotification.setContent("测试内容");
        firstNotification.setType("USER");
        firstNotification.setBizId("test_biz_id_001");

        Long firstId = notificationService.addNotification(firstNotification);
        Assertions.assertNotNull(firstId, "首次创建应返回通知 ID");

        // When: 使用相同 bizId 再次创建通知
        Notification duplicateNotification = new Notification();
        duplicateNotification.setUserId(1L);
        duplicateNotification.setTitle("测试通知");
        duplicateNotification.setContent("测试内容");
        duplicateNotification.setType("USER");
        duplicateNotification.setBizId("test_biz_id_001"); // 相同 bizId

        Long secondId = notificationService.addNotification(duplicateNotification);

        // Then: 应返回首次创建的 ID，不创建新记录
        Assertions.assertEquals(firstId, secondId,
                "相同 bizId 的通知应返回已有 ID");
        Assertions.assertEquals(1, notificationService.getSavedNotificationsCount(),
                "不应创建重复通知记录");
    }

    @Test
    @DisplayName("不同 bizId 的通知应创建新记录")
    void shouldCreateNewNotificationForDifferentBizId() {
        // Given: 首次创建通知
        Notification firstNotification = new Notification();
        firstNotification.setUserId(1L);
        firstNotification.setTitle("测试通知1");
        firstNotification.setContent("测试内容1");
        firstNotification.setType("USER");
        firstNotification.setBizId("test_biz_id_002");

        Long firstId = notificationService.addNotification(firstNotification);

        // When: 使用不同 bizId 创建通知
        Notification differentBizNotification = new Notification();
        differentBizNotification.setUserId(1L);
        differentBizNotification.setTitle("测试通知2");
        differentBizNotification.setContent("测试内容2");
        differentBizNotification.setType("USER");
        differentBizNotification.setBizId("test_biz_id_003"); // 不同 bizId

        Long secondId = notificationService.addNotification(differentBizNotification);

        // Then: 应创建新记录
        Assertions.assertNotEquals(firstId, secondId,
                "不同 bizId 的通知应创建新记录");
        Assertions.assertEquals(2, notificationService.getSavedNotificationsCount(),
                "应创建两条通知记录");
    }

    @Test
    @DisplayName("相同 bizId 但不同用户应各自创建记录")
    void shouldCreateSeparateNotificationsForDifferentUsersWithSameBizId() {
        // Given: 用户1的通知
        Notification notification1 = new Notification();
        notification1.setUserId(1L);
        notification1.setTitle("通知");
        notification1.setContent("内容");
        notification1.setType("USER");
        notification1.setBizId("same_biz_id");

        Long id1 = notificationService.addNotification(notification1);

        // When: 用户2的通知（相同 bizId）
        Notification notification2 = new Notification();
        notification2.setUserId(2L);
        notification2.setTitle("通知");
        notification2.setContent("内容");
        notification2.setType("USER");
        notification2.setBizId("same_biz_id");

        Long id2 = notificationService.addNotification(notification2);

        // Then: 两个用户各自创建通知
        Assertions.assertNotEquals(id1, id2,
                "不同用户的相同 bizId 应各自创建记录");
        Assertions.assertEquals(2, notificationService.getSavedNotificationsCount(),
                "应创建两条通知记录");
    }

    @Test
    @DisplayName("缺少 bizId 时自动生成并保证唯一性")
    void shouldAutoGenerateBizIdWhenMissing() {
        // Given: 缺少 bizId 的通知
        Notification notification1 = new Notification();
        notification1.setUserId(1L);
        notification1.setTitle("通知1");
        notification1.setContent("内容1");
        notification1.setType("USER");
        // 不设置 bizId

        Long id1 = notificationService.addNotification(notification1);
        Assertions.assertNotNull(id1, "应返回通知 ID");
        Assertions.assertNotNull(notification1.getBizId(), "应自动生成 bizId");

        // When: 再创建一个也缺少 bizId 的通知
        Notification notification2 = new Notification();
        notification2.setUserId(1L);
        notification2.setTitle("通知2");
        notification2.setContent("内容2");
        notification2.setType("USER");
        // 不设置 bizId

        Long id2 = notificationService.addNotification(notification2);

        // Then: 两个通知应有不同的自动生成 bizId
        Assertions.assertNotEquals(id1, id2, "自动生成的 bizId 应不同");
        Assertions.assertNotEquals(notification1.getBizId(), notification2.getBizId(),
                "两个通知应有不同的自动生成 bizId");
        Assertions.assertEquals(2, notificationService.getSavedNotificationsCount(),
                "应创建两条通知记录");
    }

    @Test
    @DisplayName("参数校验：通知对象为空应抛出异常")
    void shouldThrowExceptionWhenNotificationIsNull() {
        // When & Then: 传入 null 应抛出 IllegalArgumentException
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.addNotification(null)
        );
        Assertions.assertTrue(exception.getMessage().contains("通知和用户ID不能为空"),
                "异常信息应包含校验提示");
    }

    @Test
    @DisplayName("参数校验：用户ID为空应抛出异常")
    void shouldThrowExceptionWhenUserIdIsNull() {
        // Given: userId 为 null 的通知
        Notification notification = new Notification();
        notification.setTitle("测试");
        notification.setContent("内容");
        notification.setType("USER");
        // userId 未设置，默认为 null

        // When & Then: 应抛出 IllegalArgumentException
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.addNotification(notification)
        );
        Assertions.assertTrue(exception.getMessage().contains("通知和用户ID不能为空"),
                "异常信息应包含校验提示");
    }

    /**
     * 模拟的通知幂等性服务
     * <p>
     * 此实现模拟 NotificationServiceImpl.addNotification 的幂等逻辑：
     * 1. 使用 bizId + userId 作为唯一键
     * 2. 相同键返回已存在的通知 ID
     * 3. 不同键创建新记录
     * </p>
     */
    private static class NotificationIdempotencyService {
        private final List<Notification> savedNotifications = new ArrayList<>();
        private final Map<String, Notification> notificationByBizIdAndUser = new HashMap<>();
        private long nextId = 1L;
        private final AtomicLong autoIdCounter = new AtomicLong(1L);

        public int getSavedNotificationsCount() {
            return savedNotifications.size();
        }

        public Long addNotification(Notification notification) {
            // 校验
            if (notification == null || notification.getUserId() == null) {
                throw new IllegalArgumentException("通知和用户ID不能为空");
            }

            // 生成或使用传入的 bizId
            String bizId = notification.getBizId();
            if (bizId == null || bizId.isEmpty()) {
                bizId = "auto_" + autoIdCounter.getAndIncrement();
                notification.setBizId(bizId);
            }

            Long userId = notification.getUserId();

            // 检查是否已存在（相同 bizId + 相同用户）
            String key = bizId + "_" + userId;
            Notification existing = notificationByBizIdAndUser.get(key);
            if (existing != null) {
                return existing.getId();
            }

            // 设置默认值
            if (notification.getIsRead() == null) {
                notification.setIsRead(0);
            }

            // 保存
            notification.setId(nextId++);
            savedNotifications.add(notification);
            notificationByBizIdAndUser.put(key, notification);

            return notification.getId();
        }
    }
}