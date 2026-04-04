package com.stephen.cloud.api.chat.model.dto;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 好友查询请求
 *
 * @author StephenQiu30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "好友查询请求")
public class ChatFriendQueryRequest extends PageRequest implements Serializable {

    /**
     * 关键词（用户昵称）
     */
    @Schema(description = "Keywords (user nickname)")
    private String searchText;

    /**
     * 用户 ID
     */
    @Schema(description = "User ID")
    private Long userId;

    /**
     * 好友用户 ID
     */
    @Schema(description = "Friend User ID")
    private Long friendUserId;

    private static final long serialVersionUID = 1L;
}
