package com.stephen.cloud.api.chat.client;

import com.stephen.cloud.api.chat.model.dto.ChatFriendAddRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageReadRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageSendRequest;
import com.stephen.cloud.api.chat.model.dto.ChatPrivateRoomRequest;
import com.stephen.cloud.api.chat.model.vo.ChatFriendUserVO;
import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.api.chat.model.vo.ChatRoomVO;
import com.stephen.cloud.common.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 聊天服务 Feign 客户端接口
 * <p>
 * 定义了供其他微服务调用的内部接口。
 * </p>
 *
 * @author StephenQiu30
 */
@FeignClient(name = "mallchat-chat-service", path = "/api/chat", contextId = "chatFeignClient")
public interface ChatFeignClient {

    /**
     * 获取用户参与的房间列表
     *
     * @return 房间列表
     */
    @GetMapping("/room/list")
    BaseResponse<List<ChatRoomVO>> listUserChatRooms();

    /**
     * 发送聊天记录
     *
     * @param chatMessageSendRequest 发送消息请求
     * @return 消息 ID
     */
    @PostMapping("/message/send")
    BaseResponse<Long> sendMessage(@RequestBody ChatMessageSendRequest chatMessageSendRequest);

    /**
     * 获取历史消息
     *
     * @param roomId        房间 ID
     * @param lastMessageId 最后一条消息 ID
     * @param limit         限制数量
     * @return 消息列表
     */
    @GetMapping("/message/history")
    BaseResponse<List<ChatMessageVO>> listHistoryMessages(
            @RequestParam("roomId") Long roomId,
            @RequestParam(value = "lastMessageId", required = false) Long lastMessageId,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit);

    @PostMapping("/friend/add")
    BaseResponse<Boolean> addFriend(@RequestBody ChatFriendAddRequest request);

    @GetMapping("/friend/list")
    BaseResponse<List<ChatFriendUserVO>> listFriends();

    @PostMapping("/room/private")
    BaseResponse<Long> getOrCreatePrivateRoom(@RequestBody ChatPrivateRoomRequest request);

    @PostMapping("/message/read")
    BaseResponse<Boolean> markMessageRead(@RequestBody ChatMessageReadRequest request);
}
