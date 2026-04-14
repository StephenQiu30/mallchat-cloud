package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
     * 客户端消息ID
     */
    @Schema(description = "客户端消息ID，用于幂等控制", requiredMode = Schema.RequiredMode.REQUIRED, example = "pc-1710000000000-1")
    @NotNull(message = "客户端消息ID不能为空")
    private String clientMsgId;

    /**
     * 消息内容
     */
    @Schema(description = "消息内容，文本消息必填；图片/文件消息可为空", example = "Hello, World!")
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
    @Schema(description = "消息扩展内容（JSON 字符串）", example = "{\"url\":\"https://...\",\"width\":100,\"height\":100,\"size\":1024}")
    private String extra;

    /**
     * 被回复的消息ID
     */
    @Schema(description = "被回复的消息ID", example = "100")
    private Long replyMsgId;

    private static final long serialVersionUID = 1L;
}
