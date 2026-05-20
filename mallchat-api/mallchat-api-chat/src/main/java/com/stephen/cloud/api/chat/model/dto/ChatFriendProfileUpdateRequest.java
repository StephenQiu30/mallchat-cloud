package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 好友资料设置更新请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "好友资料设置更新请求")
public class ChatFriendProfileUpdateRequest implements Serializable {

    @Schema(description = "好友用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "好友用户ID不能为空")
    private Long friendUserId;

    @Schema(description = "好友备注")
    private String remarkName;

    @Schema(description = "好友分组名称")
    private String friendGroupName;

    private static final long serialVersionUID = 1L;
}
