package com.stephen.cloud.chat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.chat.model.vo.ChatGroupInfoVO;
import com.stephen.cloud.chat.model.entity.ChatGroupInfo;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 群组详情服务
 *
 * @author StephenQiu30
 */
public interface ChatGroupInfoService extends IService<ChatGroupInfo> {

    /**
     * 校验群组详情
     *
     * @param chatGroupInfo 群组详情实体
     */
    void validChatGroupInfo(ChatGroupInfo chatGroupInfo);

    /**
     * 获取群组视图类
     *
     * @param chatGroupInfo 群组详情
     * @param request       请求
     * @return {@link ChatGroupInfoVO}
     */
    ChatGroupInfoVO getChatGroupInfoVO(ChatGroupInfo chatGroupInfo, HttpServletRequest request);

    /**
     * 批量获取群组视图类
     *
     * @param chatGroupInfoList 群组详情列表
     * @param request           请求
     * @return {@link List<ChatGroupInfoVO>}
     */
    List<ChatGroupInfoVO> getChatGroupInfoVO(List<ChatGroupInfo> chatGroupInfoList, HttpServletRequest request);

    /**
     * 分页获取群组视图类
     *
     * @param chatGroupInfoPage 群组详情分页数据
     * @param request           请求
     * @return {@link Page<ChatGroupInfoVO>}
     */
    Page<ChatGroupInfoVO> getChatGroupInfoVOPage(Page<ChatGroupInfo> chatGroupInfoPage, HttpServletRequest request);

    /**
     * 初始化群组详情
     *
     * @param roomId    虚拟房间ID
     * @param groupName 群名称
     * @param userId    创建者
     */
    void initGroupInfo(Long roomId, String groupName, Long userId);
}
