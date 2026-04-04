package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 申请列表VO
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "好友申请记录VO")
public class ChatFriendApplyVO implements Serializable {

    /**
     * 申请ID
     */
    @Schema(description = "申请ID", example = "1")
    private Long id;

    /**
     * 发起用户ID
     */
    @Schema(description = "发起用户ID", example = "1")
    private Long userId;

    /**
     * 发起用户昵称
     */
    @Schema(description = "发起用户昵称", example = "Stephen")
    private String userName;

    /**
     * 发起用户头像
     */
    @Schema(description = "发起用户头像", example = "https://...")
    private String userAvatar;

    /**
     * 申请消息
     */
    @Schema(description = "申请消息", example = "你好")
    private String msg;

    /**
     * 状态：1-待处理，2-已同意，3-已忽略
     */
    @Schema(description = "状态：1-待处理，2-已同意，3-已忽略", example = "1")
    private Integer status;

    /**
     * 申请时间
     */
    @Schema(description = "申请时间")
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
