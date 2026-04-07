package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 被回复的消息简要信息
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "被回复的消息简要信息")
public class ReplyMsgVO implements Serializable {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "发送者姓名")
    private String userName;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息类型")
    private Integer type;

    private static final long serialVersionUID = 1L;
}
