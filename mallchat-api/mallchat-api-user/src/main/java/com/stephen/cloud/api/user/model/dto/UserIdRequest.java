package com.stephen.cloud.api.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户 ID 请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "用户 ID 请求")
public class UserIdRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    @Positive(message = "用户ID必须大于0")
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    public static UserIdRequest of(Long id) {
        UserIdRequest request = new UserIdRequest();
        request.setId(id);
        return request;
    }
}
