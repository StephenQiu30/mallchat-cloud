package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天消息视图对象
 *
 * @author StephenQiu30
 */
@Data
@Builder
@Schema(description = "聊天消息视图对象")
public class ChatMessageVO implements Serializable {

    /**
     * 消息ID
     */
    @Schema(description = "消息ID", example = "1")
    private Long id;

    /**
     * 房间ID
     */
    @Schema(description = "房间ID", example = "1")
    private Long roomId;

    /**
     * 发送者ID
     */
    @Schema(description = "发送者ID", example = "1")
    private Long fromUserId;

    /**
     * 发送者姓名 (冗余)
     */
    @Schema(description = "发送者姓名", example = "Stephen")
    private String fromUserName;

    /**
     * 发送者头像 (冗余)
     */
    @Schema(description = "发送者头像", example = "https://example.com/avatar.png")
    private String fromUserAvatar;

    /**
     * 消息内容
     */
    @Schema(description = "消息内容", example = "Hello, World!")
    private String content;

    /**
     * 消息类型：1-文本，2-图片，3-文件
     */
    @Schema(description = "消息类型：1-文本，2-图片，3-文件", example = "1")
    private Integer type;

    /**
     * 发送时间
     */
    @Schema(description = "发送时间")
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
