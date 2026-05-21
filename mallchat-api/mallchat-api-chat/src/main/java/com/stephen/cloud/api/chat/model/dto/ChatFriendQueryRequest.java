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
    @Schema(description = "关键词（用户昵称）")
    private String searchText;

    /**
     * 用户 ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 好友用户 ID
     */
    @Schema(description = "好友用户ID")
    private Long friendUserId;

    private static final long serialVersionUID = 1L;
}
