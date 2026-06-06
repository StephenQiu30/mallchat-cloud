package com.stephen.cloud.chat.e2e;

import com.stephen.cloud.api.chat.model.dto.ChatFriendApproveRequest;
import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.chat.model.entity.UserFriendApply;
import com.stephen.cloud.chat.service.ChatMessageService;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.chat.service.ChatSessionService;
import com.stephen.cloud.chat.service.UserFriendApplyService;
import com.stephen.cloud.chat.service.UserFriendService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * IM E2E Smoke Test - 私聊消息可靠性场景
 *
 * 覆盖：用户 A/B、好友、私聊房间、消息发送、会话状态、消息幂等性
 *
 * @author StephenQiu30
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("IM E2E Smoke: 私聊消息可靠性")
class PrivateMessageE2eTest extends E2eBaseTest {

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private UserFriendApplyService userFriendApplyService;

    @Autowired
    private UserFriendService userFriendService;

    @Autowired
    private ChatSessionService chatSessionService;

    private static final Long USER_A_ID = 10001L;
    private static final Long USER_B_ID = 10002L;
    private static final String CLIENT_MSG_ID_ORIGINAL = "e2e-test-msg-001";

    /**
     * RED 阶段验证：E2E 服务应可用
     */
    @Test
    @Order(1)
    @DisplayName("RED: E2E 服务应可用")
    void shouldHaveE2eServicesAvailable() {
        Assertions.assertNotNull(chatMessageService,
                "ChatMessageService 未注入 - E2E 测试基础设施缺失");
        Assertions.assertNotNull(chatRoomService,
                "ChatRoomService 未注入 - E2E 测试基础设施缺失");
        Assertions.assertNotNull(userFriendApplyService,
                "UserFriendApplyService 未注入 - E2E 测试基础设施缺失");
        Assertions.assertNotNull(userFriendService,
                "UserFriendService 未注入 - E2E 测试基础设施缺失");
        Assertions.assertNotNull(chatSessionService,
                "ChatSessionService 未注入 - E2E 测试基础设施缺失");
    }

    /**
     * GREEN 阶段：好友申请和通过流程
     * 场景：用户 A 发起好友申请 -> B 通过 -> 验证好友关系
     */
    @Test
    @Order(2)
    @DisplayName("GREEN: 好友申请和通过流程")
    void shouldEstablishFriendRelationship() {
        // 准备用户数据
        insertTestUser(USER_A_ID, "UserA");
        insertTestUser(USER_B_ID, "UserB");

        // 验证 A 和 B 尚未成为好友
        Assertions.assertFalse(userFriendService.isMutualFriend(USER_A_ID, USER_B_ID),
                "初始状态不应是好友");

        // A 发起好友申请
        UserFriendApply apply = new UserFriendApply();
        apply.setTargetId(USER_B_ID);
        apply.setMsg("E2E test friend request");
        Long applyId = userFriendApplyService.applyFriend(apply, USER_A_ID);
        Assertions.assertNotNull(applyId, "好友申请 ID 不应为空");

        // B 通过好友申请
        ChatFriendApproveRequest approveRequest = new ChatFriendApproveRequest();
        approveRequest.setApplyId(applyId);
        approveRequest.setStatus(2); // 2-同意
        boolean approved = userFriendApplyService.approveFriend(approveRequest, USER_B_ID);
        Assertions.assertTrue(approved, "好友申请应通过");

        // 验证好友关系已建立
        Assertions.assertTrue(userFriendService.isMutualFriend(USER_A_ID, USER_B_ID),
                "好友关系应已建立");
    }

    /**
     * GREEN 阶段：私聊房间获取测试
     */
    @Test
    @Order(3)
    @DisplayName("GREEN: 获取或创建私聊房间")
    void shouldGetOrCreatePrivateRoom() {
        // 准备用户和好友关系
        insertTestUser(USER_A_ID, "UserA");
        insertTestUser(USER_B_ID, "UserB");
        insertFriendRelation(USER_A_ID, USER_B_ID);
        insertFriendRelation(USER_B_ID, USER_A_ID);

        // 获取私聊房间
        Long roomId = chatRoomService.getOrCreatePrivateRoom(USER_B_ID, USER_A_ID);

        Assertions.assertNotNull(roomId, "私聊房间 ID 不应为空");
        Assertions.assertTrue(roomId > 0, "私聊房间 ID 应为正数");
    }

    /**
     * GREEN 阶段：消息发送测试
     */
    @Test
    @Order(4)
    @DisplayName("GREEN: A 发送文本消息给 B")
    void shouldSendTextMessage() {
        // 准备用户、好友关系和私聊房间
        insertTestUser(USER_A_ID, "UserA");
        insertTestUser(USER_B_ID, "UserB");
        insertFriendRelation(USER_A_ID, USER_B_ID);
        insertFriendRelation(USER_B_ID, USER_A_ID);
        Long roomId = chatRoomService.getOrCreatePrivateRoom(USER_B_ID, USER_A_ID);

        // A 发送文本消息
        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setFromUserId(USER_A_ID);
        message.setClientMsgId(CLIENT_MSG_ID_ORIGINAL);
        message.setType(1); // TEXT
        message.setContent("Hello from E2E test");

        ChatMessageVO result = chatMessageService.sendMessage(message, USER_A_ID);

        Assertions.assertNotNull(result, "消息发送应返回结果");
        Assertions.assertNotNull(result.getId(), "消息 ID 不应为空");
        Assertions.assertEquals(roomId, result.getRoomId(), "消息房间 ID 应匹配");
        Assertions.assertEquals("Hello from E2E test", result.getContent(), "消息内容应匹配");
    }

    /**
     * GREEN 阶段：消息幂等性测试
     */
    @Test
    @Order(5)
    @DisplayName("GREEN: 同一 clientMsgId 不重复落库")
    void shouldRejectDuplicateClientMsgId() {
        // 准备用户、好友关系和私聊房间
        insertTestUser(USER_A_ID, "UserA");
        insertTestUser(USER_B_ID, "UserB");
        insertFriendRelation(USER_A_ID, USER_B_ID);
        insertFriendRelation(USER_B_ID, USER_A_ID);
        Long roomId = chatRoomService.getOrCreatePrivateRoom(USER_B_ID, USER_A_ID);

        // A 发送第一条消息
        ChatMessage firstMessage = new ChatMessage();
        firstMessage.setRoomId(roomId);
        firstMessage.setFromUserId(USER_A_ID);
        firstMessage.setClientMsgId(CLIENT_MSG_ID_ORIGINAL);
        firstMessage.setType(1);
        firstMessage.setContent("First message");

        ChatMessageVO firstResult = chatMessageService.sendMessage(firstMessage, USER_A_ID);
        Long firstMessageId = firstResult.getId();

        // A 使用相同 clientMsgId 再次发送
        ChatMessage duplicateMessage = new ChatMessage();
        duplicateMessage.setRoomId(roomId);
        duplicateMessage.setFromUserId(USER_A_ID);
        duplicateMessage.setClientMsgId(CLIENT_MSG_ID_ORIGINAL);
        duplicateMessage.setType(1);
        duplicateMessage.setContent("Duplicate message");

        ChatMessageVO secondResult = chatMessageService.sendMessage(duplicateMessage, USER_A_ID);

        // 验证返回的是同一条消息，不是新创建的消息
        Assertions.assertEquals(firstMessageId, secondResult.getId(),
                "重复 clientMsgId 应返回已存在的消息，不创建新记录");
    }
}
