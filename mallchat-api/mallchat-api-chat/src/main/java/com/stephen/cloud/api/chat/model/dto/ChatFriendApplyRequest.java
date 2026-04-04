package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 好友申请请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "好友申请请求")
public class ChatFriendApplyRequest implements Serializable {

    /**
     * 目标用户ID
     */
    @Schema(description = "目标用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "目标用户ID不能为空")
    private Long targetId;

    /**
     * 申请消息
     */
    @Schema(description = "申请消息", example = "你好，我是...")
    @NotBlank(message = "申请消息不能为空")
    private String msg;

    private static final long serialVersionUID = 1L;
}
