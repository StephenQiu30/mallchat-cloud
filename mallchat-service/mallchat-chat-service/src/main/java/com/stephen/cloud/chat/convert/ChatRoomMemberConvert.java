package com.stephen.cloud.chat.convert;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.vo.ChatRoomMemberVO;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天室成员转换器
 *
 * @author StephenQiu30
 */
public class ChatRoomMemberConvert {

    /**
     * 对象转视图
     *
     * @param chatRoomMember 房间成员实体
     * @return 房间成员视图
     */
    public static ChatRoomMemberVO objToVo(ChatRoomMember chatRoomMember) {
        if (chatRoomMember == null) {
            return null;
        }
        ChatRoomMemberVO chatRoomMemberVO = new ChatRoomMemberVO();
        BeanUtils.copyProperties(chatRoomMember, chatRoomMemberVO);
        return chatRoomMemberVO;
    }

    /**
     * 对象列表转视图列表
     *
     * @param chatRoomMemberList 房间成员对象列表
     * @return 房间成员视图列表
     */
    public static List<ChatRoomMemberVO> getChatRoomMemberVO(List<ChatRoomMember> chatRoomMemberList) {
        if (CollUtil.isEmpty(chatRoomMemberList)) {
            return Collections.emptyList();
        }
        return chatRoomMemberList.stream().map(ChatRoomMemberConvert::objToVo).collect(Collectors.toList());
    }

    /**
     * 分页对象转视图分页对象
     *
     * @param chatRoomMemberPage 房间成员分页对象
     * @return 房间成员视图分页对象
     */
    public static Page<ChatRoomMemberVO> getChatRoomMemberVO(Page<ChatRoomMember> chatRoomMemberPage) {
        Page<ChatRoomMemberVO> chatRoomMemberVOPage = new Page<>(chatRoomMemberPage.getCurrent(), chatRoomMemberPage.getSize(), chatRoomMemberPage.getTotal());
        chatRoomMemberVOPage.setRecords(getChatRoomMemberVO(chatRoomMemberPage.getRecords()));
        return chatRoomMemberVOPage;
    }
}
