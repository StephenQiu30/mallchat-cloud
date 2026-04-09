package com.stephen.cloud.api.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户微信 App 登录请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "用户微信 App 登录请求")
public class UserAppLoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 微信 App 登录 code
     */
    @Schema(description = "微信 App 登录 code")
    private String code;

}
