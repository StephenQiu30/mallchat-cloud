package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    private Long roomId;

    /**
     * 消息内容
     */
    @Schema(description = "消息内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "Hello, World!")
    private String content;

    /**
     * 消息类型：1-文本，2-图片，3-文件
     */
    @Schema(description = "消息类型：1-文本，2-图片，3-文件", example = "1")
    private Integer type;

    private static final long serialVersionUID = 1L;
}
