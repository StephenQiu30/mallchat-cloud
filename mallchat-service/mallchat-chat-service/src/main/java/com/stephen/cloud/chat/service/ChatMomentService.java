package com.stephen.cloud.chat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.chat.model.dto.ChatMomentPublishRequest;
import com.stephen.cloud.api.chat.model.vo.ChatMomentVO;
import com.stephen.cloud.chat.model.entity.ChatMoment;

/**
 * 动态服务
 *
 * @author StephenQiu30
 */
public interface ChatMomentService extends IService<ChatMoment> {

    Long publish(Long userId, ChatMomentPublishRequest request);

    Page<ChatMomentVO> listVisibleMoments(Long userId, int current, int pageSize);

    void deleteMoment(Long userId, Long momentId);
}
