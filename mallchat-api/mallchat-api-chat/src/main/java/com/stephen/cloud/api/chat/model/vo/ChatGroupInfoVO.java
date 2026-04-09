package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 群组详情视图对象
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "群组详情视图对象")
public class ChatGroupInfoVO implements Serializable {

    /**
     * 主键
     */
    @Schema(description = "主键", example = "1")
    private Long id;

    /**
     * 房间ID
     */
    @Schema(description = "房间ID", example = "1")
    private Long roomId;

    /**
     * 群聊名称
     */
    @Schema(description = "群聊名称", example = "技术交流群")
    private String groupName;

    /**
     * 群聊头像
     */
    @Schema(description = "群聊头像", example = "https://example.com/avatar.png")
    private String groupAvatar;

    /**
     * 群公告
     */
    @Schema(description = "群公告", example = "大家友好交流")
    private String announcement;

    /**
     * 创建者用户ID
     */
    @Schema(description = "创建者用户ID", example = "1")
    private Long createUser;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
