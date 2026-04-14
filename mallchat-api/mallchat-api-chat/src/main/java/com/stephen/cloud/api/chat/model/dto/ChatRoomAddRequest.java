package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建聊天室请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "创建聊天室请求")
public class ChatRoomAddRequest implements Serializable {

    /**
     * 房间名称
     */
    @Schema(description = "房间名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "技术交流群")
    @NotBlank(message = "房间名称不能为空")
    private String name;

    /**
     * 房间头像
     */
    @Schema(description = "房间头像", example = "https://example.com/avatar.png")
    private String avatar;

    /**
     * 群公告
     */
    @Schema(description = "群公告", example = "欢迎加入交流群")
    private String announcement;

    /**
     * 初始群成员
     */
    @Schema(description = "初始群成员ID列表", example = "[2,3]")
    private List<Long> memberIds;

    private static final long serialVersionUID = 1L;
}
