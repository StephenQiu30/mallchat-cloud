package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 会话列表项VO
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "聊天会话VO")
public class ChatSessionVO implements Serializable {

    /**
     * 房间ID
     */
    @Schema(description = "房间ID", example = "1")
    private Long roomId;

    /**
     * 房间名称 (私聊为对方昵称，群聊为群名)
     */
    @Schema(description = "房间名称", example = "张三")
    private String name;

    /**
     * 房间头像
     */
    @Schema(description = "房间头像", example = "https://...")
    private String avatar;

    /**
     * 房间类型：1-群聊，2-私聊
     */
    @Schema(description = "房间类型：1-群聊，2-私聊", example = "2")
    private Integer type;

    /**
     * 最后一条消息内容
     */
    @Schema(description = "最后一条消息内容", example = "你好")
    private String lastMessage;

    /**
     * 未读数
     */
    @Schema(description = "未读数", example = "5")
    private Integer unreadCount;

    /**
     * 置顶状态：0-否，1-是
     */
    @Schema(description = "置顶状态：0-否，1-是", example = "0")
    private Integer topStatus;

    /**
     * 最后活跃时间
     */
    @Schema(description = "最后活跃时间")
    private Date activeTime;

    private static final long serialVersionUID = 1L;
}
