package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

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
    private String name;

    /**
     * 房间类型：1-群聊，2-私聊
     */
    @Schema(description = "房间类型：1-群聊，2-私聊", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer type;

    /**
     * 房间头像
     */
    @Schema(description = "房间头像", example = "https://example.com/avatar.png")
    private String avatar;

    private static final long serialVersionUID = 1L;
}
