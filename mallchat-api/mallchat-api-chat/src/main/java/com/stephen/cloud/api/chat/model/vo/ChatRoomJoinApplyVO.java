package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 入群申请视图
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "入群申请VO")
public class ChatRoomJoinApplyVO implements Serializable {

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "房间ID")
    private Long roomId;

    @Schema(description = "申请用户ID")
    private Long userId;

    @Schema(description = "审核用户ID")
    private Long reviewerId;

    @Schema(description = "申请留言")
    private String msg;

    @Schema(description = "审核留言")
    private String reviewMsg;

    @Schema(description = "状态：1-待处理，2-已同意，3-已拒绝")
    private Integer status;

    @Schema(description = "申请时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
