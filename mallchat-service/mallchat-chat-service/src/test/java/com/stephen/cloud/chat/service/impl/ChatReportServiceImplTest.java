package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.dto.ChatReportListRequest;
import com.stephen.cloud.api.chat.model.dto.ChatReportSubmitRequest;
import com.stephen.cloud.api.chat.model.enums.ChatReportTargetTypeEnum;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.chat.model.entity.ChatMoment;
import com.stephen.cloud.chat.model.entity.ChatReport;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.chat.service.UserFriendService;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ChatReportServiceImplTest {

    private TestableChatReportServiceImpl chatReportService;
    private Map<Long, UserVO> users;
    private boolean roomMember;
    private Set<Long> mutualFriendIds;
    private boolean blockedBetween;

    @BeforeEach
    void setUp() {
        chatReportService = new TestableChatReportServiceImpl();
        users = new HashMap<>();
        roomMember = true;
        mutualFriendIds = Set.of();
        blockedBetween = false;
        ReflectionTestUtils.setField(chatReportService, "userFeignClient", createUserFeignClient());
        ReflectionTestUtils.setField(chatReportService, "chatRoomMemberService", createChatRoomMemberService());
        ReflectionTestUtils.setField(chatReportService, "userFriendService", createUserFriendService());
    }

    @Test
    void shouldCreateMessageReportWhenReporterCanAccessMessageRoom() {
        ChatMessage message = new ChatMessage();
        message.setId(20L);
        message.setRoomId(10L);
        message.setFromUserId(2L);
        chatReportService.messageById = message;
        chatReportService.saveResult = true;
        ChatReportSubmitRequest request = new ChatReportSubmitRequest();
        request.setTargetType(ChatReportTargetTypeEnum.MESSAGE.getCode());
        request.setTargetId(20L);
        request.setReasonType("spam");
        request.setReason("广告消息");

        Long result = chatReportService.submitReport(1L, request);

        Assertions.assertEquals(100L, result);
        Assertions.assertEquals(1L, chatReportService.savedReport.getReporterUserId());
        Assertions.assertEquals(ChatReportTargetTypeEnum.MESSAGE.getCode(), chatReportService.savedReport.getTargetType());
        Assertions.assertEquals(20L, chatReportService.savedReport.getTargetId());
        Assertions.assertEquals(2L, chatReportService.savedReport.getTargetOwnerId());
    }

    @Test
    void shouldReturnExistingReportForDuplicateTarget() {
        ChatReport existing = new ChatReport();
        existing.setId(88L);
        chatReportService.existingReport = existing;
        ChatReportSubmitRequest request = new ChatReportSubmitRequest();
        request.setTargetType(ChatReportTargetTypeEnum.USER.getCode());
        request.setTargetId(2L);
        request.setReasonType("spam");
        users.put(2L, new UserVO());

        Long result = chatReportService.submitReport(1L, request);

        Assertions.assertEquals(88L, result);
        Assertions.assertNull(chatReportService.savedReport);
    }

    @Test
    void shouldRejectMessageReportWhenReporterCannotAccessRoom() {
        roomMember = false;
        ChatMessage message = new ChatMessage();
        message.setId(20L);
        message.setRoomId(10L);
        chatReportService.messageById = message;
        ChatReportSubmitRequest request = new ChatReportSubmitRequest();
        request.setTargetType(ChatReportTargetTypeEnum.MESSAGE.getCode());
        request.setTargetId(20L);
        request.setReasonType("spam");

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatReportService.submitReport(1L, request));

        Assertions.assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        Assertions.assertNull(chatReportService.savedReport);
    }

    @Test
    void shouldRejectSelfUserReport() {
        ChatReportSubmitRequest request = new ChatReportSubmitRequest();
        request.setTargetType(ChatReportTargetTypeEnum.USER.getCode());
        request.setTargetId(1L);
        request.setReasonType("spam");

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatReportService.submitReport(1L, request));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        Assertions.assertNull(chatReportService.savedReport);
    }

    @Test
    void shouldCreateMomentReportWhenReporterCanViewMomentAuthor() {
        ChatMoment moment = new ChatMoment();
        moment.setId(30L);
        moment.setUserId(2L);
        moment.setStatus(0);
        moment.setVisibility(0);
        moment.setAuditStatus(1);
        moment.setIsDelete(0);
        chatReportService.momentById = moment;
        chatReportService.saveResult = true;
        mutualFriendIds = Set.of(2L);
        ChatReportSubmitRequest request = new ChatReportSubmitRequest();
        request.setTargetType(ChatReportTargetTypeEnum.MOMENT.getCode());
        request.setTargetId(30L);
        request.setReasonType("illegal");

        Long result = chatReportService.submitReport(1L, request);

        Assertions.assertEquals(100L, result);
        Assertions.assertEquals(ChatReportTargetTypeEnum.MOMENT.getCode(), chatReportService.savedReport.getTargetType());
        Assertions.assertEquals(2L, chatReportService.savedReport.getTargetOwnerId());
    }

    @Test
    void shouldCreateMomentReportWhenReporterCanViewPublicMoment() {
        ChatMoment moment = publicMoment(30L, 2L);
        chatReportService.momentById = moment;
        chatReportService.saveResult = true;
        ChatReportSubmitRequest request = new ChatReportSubmitRequest();
        request.setTargetType(ChatReportTargetTypeEnum.MOMENT.getCode());
        request.setTargetId(30L);
        request.setReasonType("illegal");

        Long result = chatReportService.submitReport(1L, request);

        Assertions.assertEquals(100L, result);
        Assertions.assertEquals(ChatReportTargetTypeEnum.MOMENT.getCode(), chatReportService.savedReport.getTargetType());
        Assertions.assertEquals(2L, chatReportService.savedReport.getTargetOwnerId());
    }

    @Test
    void shouldRejectPublicMomentReportWhenReporterBlockedWithAuthor() {
        blockedBetween = true;
        chatReportService.momentById = publicMoment(30L, 2L);
        ChatReportSubmitRequest request = new ChatReportSubmitRequest();
        request.setTargetType(ChatReportTargetTypeEnum.MOMENT.getCode());
        request.setTargetId(30L);
        request.setReasonType("illegal");

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatReportService.submitReport(1L, request));

        Assertions.assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        Assertions.assertNull(chatReportService.savedReport);
    }

    @Test
    void shouldReturnExistingReportWhenUniqueKeyRaceHappens() {
        ChatMessage message = new ChatMessage();
        message.setId(20L);
        message.setRoomId(10L);
        message.setFromUserId(2L);
        chatReportService.messageById = message;
        chatReportService.throwDuplicateOnSave = true;
        ChatReport duplicate = new ChatReport();
        duplicate.setId(77L);
        chatReportService.duplicateAfterSave = duplicate;
        ChatReportSubmitRequest request = new ChatReportSubmitRequest();
        request.setTargetType(ChatReportTargetTypeEnum.MESSAGE.getCode());
        request.setTargetId(20L);
        request.setReasonType("spam");

        Long result = chatReportService.submitReport(1L, request);

        Assertions.assertEquals(77L, result);
    }

    // ========== STE-190: Admin report list query ==========

    @Test
    void shouldReturnReportListForAdmin() {
        ChatReport report1 = buildReport(1L, 1L, 2, 20L, 2L);
        ChatReport report2 = buildReport(2L, 3L, 1, 5L, 5L);
        chatReportService.pageResult.setRecords(List.of(report1, report2));
        chatReportService.pageResult.setTotal(2);

        ChatReportListRequest request = new ChatReportListRequest();
        request.setCurrent(1);
        request.setPageSize(20);
        var result = chatReportService.listReports(request);

        Assertions.assertEquals(2, result.getTotal());
        Assertions.assertEquals(2, result.getRecords().size());
        Assertions.assertEquals(1L, result.getRecords().get(0).getId());
        Assertions.assertEquals(2L, result.getRecords().get(1).getId());
    }

    @Test
    void shouldReturnEmptyReportListWhenNoReports() {
        chatReportService.pageResult.setRecords(List.of());
        chatReportService.pageResult.setTotal(0);

        ChatReportListRequest request = new ChatReportListRequest();
        request.setCurrent(1);
        request.setPageSize(20);
        var result = chatReportService.listReports(request);

        Assertions.assertEquals(0, result.getTotal());
        Assertions.assertTrue(result.getRecords().isEmpty());
    }

    private ChatReport buildReport(Long id, Long reporterUserId, Integer targetType, Long targetId, Long targetOwnerId) {
        ChatReport report = new ChatReport();
        report.setId(id);
        report.setReporterUserId(reporterUserId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setTargetOwnerId(targetOwnerId);
        report.setReasonType("spam");
        report.setReason("test reason");
        report.setStatus(0);
        report.setCreateTime(new Date());
        return report;
    }

    private UserFeignClient createUserFeignClient() {
        return (UserFeignClient) Proxy.newProxyInstance(
                UserFeignClient.class.getClassLoader(),
                new Class[]{UserFeignClient.class},
                (proxy, method, args) -> {
                    if ("getUserVOById".equals(method.getName())) {
                        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), users.get((Long) args[0]), "ok");
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private ChatRoomMemberService createChatRoomMemberService() {
        return (ChatRoomMemberService) Proxy.newProxyInstance(
                ChatRoomMemberService.class.getClassLoader(),
                new Class[]{ChatRoomMemberService.class},
                (proxy, method, args) -> {
                    if ("isMember".equals(method.getName())) {
                        return roomMember;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private UserFriendService createUserFriendService() {
        return (UserFriendService) Proxy.newProxyInstance(
                UserFriendService.class.getClassLoader(),
                new Class[]{UserFriendService.class},
                (proxy, method, args) -> {
                    if ("listMutualFriendIds".equals(method.getName())) {
                        return mutualFriendIds;
                    }
                    if ("isBlockedBetween".equals(method.getName())) {
                        return blockedBetween;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static ChatMoment publicMoment(Long id, Long userId) {
        ChatMoment moment = new ChatMoment();
        moment.setId(id);
        moment.setUserId(userId);
        moment.setStatus(0);
        moment.setVisibility(1);
        moment.setAuditStatus(1);
        moment.setIsDelete(0);
        return moment;
    }

    private static class TestableChatReportServiceImpl extends ChatReportServiceImpl {
        private ChatReport existingReport;
        private ChatReport duplicateAfterSave;
        private ChatMessage messageById;
        private ChatMoment momentById;
        private ChatReport savedReport;
        private boolean saveResult;
        private boolean throwDuplicateOnSave;
        private boolean saveAttempted;
        private Page<ChatReport> pageResult = new Page<>(1, 20, 0);

        @Override
        public ChatReport getOne(Wrapper<ChatReport> queryWrapper) {
            if (saveAttempted && duplicateAfterSave != null) {
                return duplicateAfterSave;
            }
            return existingReport;
        }

        @Override
        protected ChatMessage getMessageById(Long messageId) {
            return messageById;
        }

        @Override
        protected ChatMoment getMomentById(Long momentId) {
            return momentById;
        }

        @Override
        public boolean save(ChatReport entity) {
            saveAttempted = true;
            if (throwDuplicateOnSave) {
                throw new DuplicateKeyException("duplicate report");
            }
            if (saveResult && entity.getId() == null) {
                entity.setId(100L);
            }
            this.savedReport = entity;
            return saveResult;
        }

        @Override
        public <E extends IPage<ChatReport>> E page(E page, Wrapper<ChatReport> queryWrapper) {
            @SuppressWarnings("unchecked")
            E result = (E) pageResult;
            return result;
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }
}
