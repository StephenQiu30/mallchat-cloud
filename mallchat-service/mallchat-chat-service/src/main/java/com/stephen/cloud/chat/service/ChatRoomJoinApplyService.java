package com.stephen.cloud.chat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.chat.model.dto.ChatRoomJoinApplyRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomJoinApproveRequest;
import com.stephen.cloud.api.chat.model.vo.ChatRoomJoinApplyVO;
import com.stephen.cloud.chat.model.entity.ChatRoomJoinApply;

/**
 * 入群申请服务
 *
 * @author StephenQiu30
 */
public interface ChatRoomJoinApplyService extends IService<ChatRoomJoinApply> {

    Long applyJoinRoom(ChatRoomJoinApplyRequest request, Long userId);

    boolean approveJoinRoom(ChatRoomJoinApproveRequest request, Long reviewerId);

    Page<ChatRoomJoinApplyVO> listRoomJoinApplyPage(Long roomId, long current, long size, Long reviewerId);
}
