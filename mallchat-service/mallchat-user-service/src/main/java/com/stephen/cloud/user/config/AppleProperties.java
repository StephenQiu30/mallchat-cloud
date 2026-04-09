package com.stephen.cloud.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Apple 登录配置
 *
 * @author stephen
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "apple")
public class AppleProperties {

    /**
     * Apple Client ID (Bundle ID)
     */
    private String clientId;

}
