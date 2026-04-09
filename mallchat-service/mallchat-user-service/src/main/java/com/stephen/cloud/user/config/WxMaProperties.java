package com.stephen.cloud.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信小程序配置
 *
 * @author stephen
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wx.ma")
public class WxMaProperties {

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
