package com.stephen.cloud.chat.convert;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.vo.ChatRoomJoinApplyVO;
import com.stephen.cloud.chat.model.entity.ChatRoomJoinApply;
import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;

/**
 * 入群申请转换器
 *
 * @author StephenQiu30
 */
public class ChatRoomJoinApplyConvert {

    public static ChatRoomJoinApplyVO objToVo(ChatRoomJoinApply apply) {
        if (apply == null) {
            return null;
        }
        ChatRoomJoinApplyVO vo = new ChatRoomJoinApplyVO();
        BeanUtils.copyProperties(apply, vo);
        return vo;
    }

    public static List<ChatRoomJoinApplyVO> getVOList(List<ChatRoomJoinApply> applies) {
        if (CollUtil.isEmpty(applies)) {
            return Collections.emptyList();
        }
        return applies.stream().map(ChatRoomJoinApplyConvert::objToVo).toList();
    }

    public static Page<ChatRoomJoinApplyVO> getVOPage(Page<ChatRoomJoinApply> page) {
        Page<ChatRoomJoinApplyVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(getVOList(page.getRecords()));
        return voPage;
    }
}
