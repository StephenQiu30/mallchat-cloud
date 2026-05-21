package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 好友列表请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "好友列表请求")
public class ChatFriendListRequest implements Serializable {

    @Schema(description = "好友分组名称")
    private String friendGroupName;

    private static final long serialVersionUID = 1L;
}
