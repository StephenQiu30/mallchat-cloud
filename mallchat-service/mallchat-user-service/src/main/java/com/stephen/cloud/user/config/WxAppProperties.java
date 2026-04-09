package com.stephen.cloud.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信 App 配置 (支持 Flutter 端)
 *
 * @author stephen
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wx.app")
public class WxAppProperties {

    /**
     * App ID
     */
    private String appId;

    /**
     * App Secret
     */
    private String appSecret;

}
