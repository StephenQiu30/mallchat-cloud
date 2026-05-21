package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Chat 资源 ID 响应
 *
 * @author StephenQiu30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat 资源 ID 响应")
public class ChatIdVO implements Serializable {

    @Schema(description = "资源ID")
    private Long id;

    public static ChatIdVO of(Long id) {
        return new ChatIdVO(id);
    }

    private static final long serialVersionUID = 1L;
}
