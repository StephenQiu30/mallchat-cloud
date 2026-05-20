package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "好友用户简要信息")
public class ChatFriendUserVO implements Serializable {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "昵称")
    private String userName;

    @Schema(description = "头像")
    private String userAvatar;

    @Schema(description = "好友备注")
    private String remarkName;

    @Schema(description = "好友分组名称")
    private String friendGroupName;

    @Schema(description = "在线状态：0-离线，1-在线")
    private Integer onlineStatus;

    @Schema(description = "关系状态：0-陌生人，1-本人，2-已是好友，3-我已发起待处理，4-对方已发起待处理")
    private Integer friendStatus;

    private static final long serialVersionUID = 1L;
}
