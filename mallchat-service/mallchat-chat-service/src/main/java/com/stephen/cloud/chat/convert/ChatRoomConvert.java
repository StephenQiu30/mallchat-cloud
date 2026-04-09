package com.stephen.cloud.chat.convert;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.dto.ChatRoomAddRequest;
import com.stephen.cloud.api.chat.model.vo.ChatRoomVO;
import com.stephen.cloud.chat.model.entity.ChatRoom;
import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天室转换器
 *
 * @author StephenQiu30
 */
public class ChatRoomConvert {

    /**
     * 对象转视图
     *
     * @param chatRoom 聊天室实体
     * @return 聊天室视图
     */
    public static ChatRoomVO objToVo(ChatRoom chatRoom) {
        if (chatRoom == null) {
            return null;
        }
        ChatRoomVO chatRoomVO = new ChatRoomVO();
        BeanUtils.copyProperties(chatRoom, chatRoomVO);
        return chatRoomVO;
    }

    /**
     * 对象列表转视图列表
     *
     * @param chatRoomList 聊天室对象列表
     * @return 聊天室视图列表
     */
    public static List<ChatRoomVO> getChatRoomVO(List<ChatRoom> chatRoomList) {
        if (CollUtil.isEmpty(chatRoomList)) {
            return Collections.emptyList();
        }
        return chatRoomList.stream().map(ChatRoomConvert::objToVo).collect(Collectors.toList());
    }

    /**
     * 分页对象转视图分页对象
     *
     * @param chatRoomPage 聊天室分页对象
     * @return 聊天室视图分页对象
     */
    public static Page<ChatRoomVO> getChatRoomVO(Page<ChatRoom> chatRoomPage) {
        Page<ChatRoomVO> chatRoomVOPage = new Page<>(chatRoomPage.getCurrent(), chatRoomPage.getSize(), chatRoomPage.getTotal());
        chatRoomVOPage.setRecords(getChatRoomVO(chatRoomPage.getRecords()));
        return chatRoomVOPage;
    }

    /**
     * 创建请求转实体对象
     *
     * @param chatRoomAddRequest 创建请求
     * @return 聊天室实体
     */
    public static ChatRoom addRequestToObj(ChatRoomAddRequest chatRoomAddRequest) {
        if (chatRoomAddRequest == null) {
            return null;
        }
        ChatRoom chatRoom = new ChatRoom();
        BeanUtils.copyProperties(chatRoomAddRequest, chatRoom);
        return chatRoom;
    }
}
