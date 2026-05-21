package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Chat 操作结果响应
 *
 * @author StephenQiu30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat 操作结果响应")
public class ChatOperationResultVO implements Serializable {

    @Schema(description = "是否成功")
    private Boolean success;

    public static ChatOperationResultVO of(Boolean success) {
        return new ChatOperationResultVO(success);
    }

    private static final long serialVersionUID = 1L;
}
