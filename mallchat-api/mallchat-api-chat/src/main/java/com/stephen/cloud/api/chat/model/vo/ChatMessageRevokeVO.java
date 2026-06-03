package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 消息撤回记录视图对象
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "消息撤回记录视图对象")
public class ChatMessageRevokeVO implements Serializable {

    @Schema(description = "撤回记录ID", example = "1")
    private Long id;

    @Schema(description = "消息ID", example = "100")
    private Long messageId;

    @Schema(description = "撤回者ID", example = "1")
    private Long revokerId;

    @Schema(description = "撤回时间")
    private Date revokedAt;

    @Schema(description = "撤回原因", example = "发错了")
    private String reason;

    private static final long serialVersionUID = 1L;
}
