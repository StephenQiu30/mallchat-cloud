package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.chat.model.vo.ChatGroupInfoVO;
import com.stephen.cloud.chat.convert.ChatGroupInfoConvert;
import com.stephen.cloud.chat.mapper.ChatGroupInfoMapper;
import com.stephen.cloud.chat.model.entity.ChatGroupInfo;
import com.stephen.cloud.chat.service.ChatGroupInfoService;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 群组详情服务项
 *
 * @author StephenQiu30
 */
@Service
public class ChatGroupInfoServiceImpl extends ServiceImpl<ChatGroupInfoMapper, ChatGroupInfo>
        implements ChatGroupInfoService {

    /**
     * 校验群组详情
     *
     * @param chatGroupInfo 群组详情实体
     */
    @Override
    public void validChatGroupInfo(ChatGroupInfo chatGroupInfo) {
        if (chatGroupInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 核心权限或业务校验
        if (StringUtils.isNotBlank(chatGroupInfo.getGroupName())) {
            ThrowUtils.throwIf(chatGroupInfo.getGroupName().length() > 80, ErrorCode.PARAMS_ERROR, "群名称过长");
        }
    }

    @Override
    public ChatGroupInfoVO getChatGroupInfoVO(ChatGroupInfo chatGroupInfo, HttpServletRequest request) {
        return ChatGroupInfoConvert.objToVo(chatGroupInfo);
    }

    @Override
    public List<ChatGroupInfoVO> getChatGroupInfoVO(List<ChatGroupInfo> chatGroupInfoList, HttpServletRequest request) {
        return ChatGroupInfoConvert.getChatGroupInfoVO(chatGroupInfoList);
    }

    @Override
    public Page<ChatGroupInfoVO> getChatGroupInfoVOPage(Page<ChatGroupInfo> chatGroupInfoPage, HttpServletRequest request) {
        return ChatGroupInfoConvert.getChatGroupInfoVO(chatGroupInfoPage);
    }

    @Override
    public void initGroupInfo(Long roomId, String groupName, String groupAvatar, String announcement, Long userId) {
        ChatGroupInfo groupInfo = new ChatGroupInfo();
        groupInfo.setRoomId(roomId);
        groupInfo.setGroupName(groupName);
        groupInfo.setGroupAvatar(groupAvatar);
        groupInfo.setAnnouncement(announcement);
        groupInfo.setCreateUser(userId);
        this.save(groupInfo);
    }
}
