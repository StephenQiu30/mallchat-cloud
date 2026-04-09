package com.stephen.cloud.api.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户微信小程序登录请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "用户微信小程序登录请求")
public class UserMaLoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 微信小程序登录 code
     */
    @Schema(description = "微信小程序登录 code")
    private String code;

}
