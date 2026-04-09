package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天室成员视图对象
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天室成员视图对象")
public class ChatRoomMemberVO implements Serializable {

    /**
     * 成员ID
     */
    @Schema(description = "成员ID", example = "1")
    private Long id;

    /**
     * 房间ID
     */
    @Schema(description = "房间ID", example = "1")
    private Long roomId;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    /**
     * 用户名称
     */
    @Schema(description = "用户名称", example = "Stephen")
    private String userName;

    /**
     * 用户头像
     */
    @Schema(description = "用户头像", example = "https://example.com/avatar.png")
    private String userAvatar;

    /**
     * 角色：1-普通成员，2-管理员，3-群主
     */
    @Schema(description = "角色：1-普通成员，2-管理员，3-群主", example = "1")
    private Integer role;

    /**
     * 最后已读消息ID
     */
    @Schema(description = "最后已读消息ID", example = "100")
    private Long lastReadMessageId;

    /**
     * 加入时间
     */
    @Schema(description = "加入时间")
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
