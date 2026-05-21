package com.stephen.cloud.api.chat.client;

import com.stephen.cloud.api.chat.model.dto.ChatFriendAddRequest;
import com.stephen.cloud.api.chat.model.dto.ChatFriendDeleteRequest;
import com.stephen.cloud.api.chat.model.dto.ChatFriendListRequest;
import com.stephen.cloud.api.chat.model.dto.ChatFriendQueryRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageAfterQueryRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageHistoryQueryRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageReadRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageReadStatusRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageSendRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.dto.ChatPrivateRoomRequest;
import com.stephen.cloud.api.chat.model.vo.ChatFriendUserVO;
import com.stephen.cloud.api.chat.model.vo.ChatIdVO;
import com.stephen.cloud.api.chat.model.vo.ChatMessageReadStatusVO;
import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.api.chat.model.vo.ChatOperationResultVO;
import com.stephen.cloud.api.chat.model.vo.ChatRoomVO;
import com.stephen.cloud.common.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;

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
    @GetMapping("/room/list/vo")
    BaseResponse<List<ChatRoomVO>> listUserChatRooms();

    /**
     * 发送聊天记录
     *
     * @param chatMessageSendRequest 发送消息请求
     * @return 消息视图
     */
    @PostMapping("/message/send")
    BaseResponse<ChatMessageVO> sendMessage(@RequestBody ChatMessageSendRequest chatMessageSendRequest);

    /**
     * 获取历史消息
     *
     * @param request 查询请求
     * @return 消息列表
     */
    @GetMapping("/message/list/history/vo")
    BaseResponse<List<ChatMessageVO>> listHistoryMessages(@SpringQueryMap ChatMessageHistoryQueryRequest request);

    /**
     * 获取接收游标后的新消息
     *
     * @param request 查询请求
     * @return 消息列表
     */
    @GetMapping("/message/list/after/vo")
    BaseResponse<List<ChatMessageVO>> listMessagesAfter(@SpringQueryMap ChatMessageAfterQueryRequest request);

    @PostMapping("/friend/add")
    BaseResponse<ChatOperationResultVO> addFriend(@RequestBody ChatFriendAddRequest request);

    @DeleteMapping("/friend/delete")
    BaseResponse<ChatOperationResultVO> deleteFriend(@SpringQueryMap ChatFriendDeleteRequest request);

    @GetMapping("/friend/list/vo")
    BaseResponse<List<ChatFriendUserVO>> listFriends(@SpringQueryMap ChatFriendListRequest request);

    @GetMapping("/friend/search")
    BaseResponse<Page<ChatFriendUserVO>> searchFriends(@SpringQueryMap ChatFriendQueryRequest request);

    @PostMapping("/room/private")
    BaseResponse<ChatIdVO> getOrCreatePrivateRoom(@RequestBody ChatPrivateRoomRequest request);

    @PostMapping("/message/read")
    BaseResponse<ChatOperationResultVO> markMessageRead(@RequestBody ChatMessageReadRequest request);

    @GetMapping("/message/read/status")
    BaseResponse<ChatMessageReadStatusVO> getMessageReadStatus(@SpringQueryMap ChatMessageReadStatusRequest request);
}
