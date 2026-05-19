package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 群聊资料更新请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "群聊资料更新请求")
public class ChatRoomUpdateRequest implements Serializable {

    /**
     * 房间 ID
     */
    @Schema(description = "房间ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "群聊ID不能为空")
    private Long roomId;

    /**
     * 群聊名称
     */
    @Schema(description = "群聊名称", example = "技术交流群")
    private String name;

    /**
     * 群聊头像
     */
    @Schema(description = "群聊头像", example = "https://example.com/avatar.png")
    private String avatar;

    /**
     * 群公告
     */
    @Schema(description = "群公告", example = "欢迎加入交流群")
    private String announcement;

    private static final long serialVersionUID = 1L;
}
