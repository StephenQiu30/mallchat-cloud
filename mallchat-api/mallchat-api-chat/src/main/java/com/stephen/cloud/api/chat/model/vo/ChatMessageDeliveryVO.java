package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 消息投递状态视图对象
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "消息投递状态视图对象")
public class ChatMessageDeliveryVO implements Serializable {

    @Schema(description = "投递记录ID", example = "1")
    private Long id;

    @Schema(description = "消息ID", example = "100")
    private Long messageId;

    @Schema(description = "接收用户ID", example = "1")
    private Long userId;

    /**
     * 投递状态：0-待投递，1-已投递，2-投递失败
     */
    @Schema(description = "投递状态：0-待投递，1-已投递，2-投递失败", example = "0")
    private Integer status;

    @Schema(description = "投递状态描述", example = "待投递")
    private String statusDesc;

    @Schema(description = "重试次数", example = "0")
    private Integer retryCount;

    @Schema(description = "最后重试时间")
    private Date lastRetryAt;

    @Schema(description = "创建时间")
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
