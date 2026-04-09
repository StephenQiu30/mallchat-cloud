package com.stephen.cloud.api.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户 Apple 登录请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "用户 Apple 登录请求")
public class UserAppleLoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Apple Identity Token (JWT)
     */
    @Schema(description = "Apple Identity Token (JWT)")
    @NotBlank(message = "Identity Token 不能为空")
    private String identityToken;

    /**
     * Apple 用户标识 (User Identifier)
     */
    @Schema(description = "Apple 用户标识 (User Identifier)")
    @NotBlank(message = "用户标识不能为空")
    private String userIdentifier;
}
