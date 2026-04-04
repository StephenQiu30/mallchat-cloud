package com.stephen.cloud.chat.convert;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.vo.ChatSessionVO;
import com.stephen.cloud.chat.model.entity.ChatSession;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天会话转换器
 *
 * @author StephenQiu30
 */
public class ChatSessionConvert {

    /**
     * 对象转视图
     *
     * @param chatSession 聊天会话实体
     * @return 聊天会话视图
     */
    public static ChatSessionVO objToVo(ChatSession chatSession) {
        if (chatSession == null) {
            return null;
        }
        ChatSessionVO chatSessionVO = new ChatSessionVO();
        BeanUtils.copyProperties(chatSession, chatSessionVO);
        return chatSessionVO;
    }

    /**
     * 对象列表转视图列表
     *
     * @param chatSessionList 聊天会话对象列表
     * @return 聊天会话视图列表
     */
    public static List<ChatSessionVO> getChatSessionVO(List<ChatSession> chatSessionList) {
        return chatSessionList.stream().map(ChatSessionConvert::objToVo).collect(Collectors.toList());
    }

    /**
     * 分页对象转视图分页对象
     *
     * @param chatSessionPage 聊天会话分页对象
     * @return 聊天会话视图分页对象
     */
    public static Page<ChatSessionVO> getChatSessionVO(Page<ChatSession> chatSessionPage) {
        Page<ChatSessionVO> chatSessionVOPage = new Page<>(chatSessionPage.getCurrent(), chatSessionPage.getSize(), chatSessionPage.getTotal());
        chatSessionVOPage.setRecords(getChatSessionVO(chatSessionPage.getRecords()));
        return chatSessionVOPage;
    }
}
