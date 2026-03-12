package com.stephen.cloud.api.user.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 微信扫码登录响应
 *
 * @author stephen
 */
@Data
@Builder
@Schema(description = "微信扫码登录响应")
public class WxLoginResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 二维码 URL
     */
    @Schema(description = "二维码 URL")
    private String qrCodeUrl;

    /**
     * 场景 ID
     */
    @Schema(description = "场景 ID")
    private String sceneId;
}
