package com.stephen.cloud.api.chat.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

/**
 * 群治理请求（缓存一致性、治理指标、审计日志共用）
 *
 * @author StephenQiu30
 */
@Data
public class ChatRoomGovernanceRequest implements Serializable {
    /**
     * 房间 ID
     */
    @NotNull(message = "房间 ID 不能为空")
    @Positive(message = "房间 ID 必须为正数")
    private Long roomId;

    /**
     * 页码（审计日志分页用，默认 1）
     */
    private Integer pageNum;

    /**
     * 每页大小（审计日志分页用，默认 20）
     */
    private Integer pageSize;
}
