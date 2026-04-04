package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 发送消息请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "发送消息请求")
public class ChatMessageSendRequest implements Serializable {

    /**
     * 房间ID
     */
    @Schema(description = "房间ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    /**
     * 消息内容
     */
    @Schema(description = "消息内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "Hello, World!")
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /**
     * 消息类型：1-文本，2-图片，3-文件
     */
    @Schema(description = "消息类型：1-文本，2-图片，3-文件", example = "1")
    @NotNull(message = "消息类型不能为空")
    private Integer type;

    /**
     * 消息扩展内容（JSON 字符串）
     */
    @Schema(description = "消息扩展内容（JSON 字符串）", example = "{\"url\":\"...\"}")
    private String extra;

    private static final long serialVersionUID = 1L;
}
