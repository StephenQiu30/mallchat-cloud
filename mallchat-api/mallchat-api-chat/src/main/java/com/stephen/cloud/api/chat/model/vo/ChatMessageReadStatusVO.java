package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 消息已读统计摘要
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "消息已读统计摘要")
public class ChatMessageReadStatusVO implements Serializable {

    @Schema(description = "房间ID")
    private Long roomId;

    @Schema(description = "消息ID")
    private Long messageId;

    @Schema(description = "当前房间成员总数")
    private Integer totalCount;

    @Schema(description = "已读成员数")
    private Integer readCount;

    @Schema(description = "未读成员数")
    private Integer unreadCount;

    private static final long serialVersionUID = 1L;
}
