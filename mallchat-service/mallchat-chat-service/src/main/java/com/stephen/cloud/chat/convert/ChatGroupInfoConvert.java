package com.stephen.cloud.chat.convert;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.vo.ChatGroupInfoVO;
import com.stephen.cloud.chat.model.entity.ChatGroupInfo;
import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 群组详情转换器
 *
 * @author StephenQiu30
 */
public class ChatGroupInfoConvert {

    /**
     * 对象转视图
     *
     * @param chatGroupInfo 群组详情实体
     * @return 群组详情视图
     */
    public static ChatGroupInfoVO objToVo(ChatGroupInfo chatGroupInfo) {
        if (chatGroupInfo == null) {
            return null;
        }
        ChatGroupInfoVO chatGroupInfoVO = new ChatGroupInfoVO();
        BeanUtils.copyProperties(chatGroupInfo, chatGroupInfoVO);
        return chatGroupInfoVO;
    }

    /**
     * 对象列表转视图列表
     *
     * @param chatGroupInfoList 群组详情对象列表
     * @return 群组详情视图列表
     */
    public static List<ChatGroupInfoVO> getChatGroupInfoVO(List<ChatGroupInfo> chatGroupInfoList) {
        if (CollUtil.isEmpty(chatGroupInfoList)) {
            return Collections.emptyList();
        }
        return chatGroupInfoList.stream().map(ChatGroupInfoConvert::objToVo).collect(Collectors.toList());
    }

    /**
     * 分页对象转视图分页对象
     *
     * @param chatGroupInfoPage 群组详情分页对象
     * @return 群组详情视图分页对象
     */
    public static Page<ChatGroupInfoVO> getChatGroupInfoVO(Page<ChatGroupInfo> chatGroupInfoPage) {
        Page<ChatGroupInfoVO> chatGroupInfoVOPage = new Page<>(chatGroupInfoPage.getCurrent(), chatGroupInfoPage.getSize(), chatGroupInfoPage.getTotal());
        chatGroupInfoVOPage.setRecords(getChatGroupInfoVO(chatGroupInfoPage.getRecords()));
        return chatGroupInfoVOPage;
    }
}
