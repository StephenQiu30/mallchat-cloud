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

    private static final long serialVersionUID = 1L;
}
