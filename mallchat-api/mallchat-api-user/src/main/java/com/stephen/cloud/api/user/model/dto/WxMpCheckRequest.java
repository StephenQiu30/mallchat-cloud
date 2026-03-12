package com.stephen.cloud.api.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 微信公众号校验请求
 *
 * @author stephen
 */
@Data
@Schema(description = "微信公众号校验请求")
public class WxMpCheckRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 时间戳
     * 微信服务器请求时的时间戳
     */
    @Schema(description = "时间戳")
    private String timestamp;

    /**
     * 随机数
     * 微信服务器请求时的随机数
     */
    @Schema(description = "随机数")
    private String nonce;

    /**
     * 签名
     * 微信服务器请求时的签名
     */
    @Schema(description = "签名")
    private String signature;

    /**
     * 随机字符串
     * 微信服务器请求时的随机字符串
     */
    @Schema(description = "随机字符串")
    private String echostr;
}
