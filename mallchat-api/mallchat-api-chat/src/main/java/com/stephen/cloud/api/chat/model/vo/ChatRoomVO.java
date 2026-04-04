package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天室视图对象
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天室视图对象")
public class ChatRoomVO implements Serializable {

    /**
     * 房间ID
     */
    @Schema(description = "房间ID", example = "1")
    private Long id;

    /**
     * 房间名称
     */
    @Schema(description = "房间名称", example = "技术交流群")
    private String name;

    /**
     * 房间类型：1-群聊，2-私聊
     */
    @Schema(description = "房间类型：1-群聊，2-私聊", example = "1")
    private Integer type;

    /**
     * 房间头像
     */
    @Schema(description = "房间头像", example = "https://example.com/avatar.png")
    private String avatar;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
