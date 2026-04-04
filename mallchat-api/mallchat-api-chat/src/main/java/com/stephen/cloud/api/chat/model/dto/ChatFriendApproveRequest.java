package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 好友审核请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "好友审核请求")
public class ChatFriendApproveRequest implements Serializable {

    /**
     * 申请记录ID
     */
    @Schema(description = "申请记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long applyId;

    /**
     * 审核状态：2-同意，3-拒绝
     */
    @Schema(description = "审核状态：2-同意，3-拒绝", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer status;

    private static final long serialVersionUID = 1L;
}
