package com.stephen.cloud.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.chat.model.dto.ChatReportSubmitRequest;
import com.stephen.cloud.chat.model.entity.ChatReport;

/**
 * 聊天举报服务
 *
 * @author StephenQiu30
 */
public interface ChatReportService extends IService<ChatReport> {

    /**
     * 提交举报
     *
     * @param reporterUserId 举报用户 ID
     * @param request        举报请求
     * @return 举报 ID
     */
    Long submitReport(Long reporterUserId, ChatReportSubmitRequest request);
}
