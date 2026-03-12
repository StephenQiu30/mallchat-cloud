package com.stephen.cloud.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信公众号配置
 *
 * @author stephen
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wx.mp")
public class WxMpProperties {

    /**
     * App ID
     */
    private String appId;

    /**
     * App Secret
     */
    private String appSecret;

    /**
     * Token
     */
    private String token;

    /**
     * AES Key
     */
    private String aesKey;
}
