package com.stephen.cloud.user.config;

import jakarta.annotation.Resource;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信公众号配置
 *
 * @author stephen
 */
@Configuration
public class WxMpConfig {

    @Resource
    private WxMpProperties wxMpProperties;

    @Bean
    public WxMpService wxMpService() {
        WxMpDefaultConfigImpl config = new WxMpDefaultConfigImpl();
        config.setAppId(wxMpProperties.getAppId());
        config.setSecret(wxMpProperties.getAppSecret());
        config.setToken(wxMpProperties.getToken());
        config.setAesKey(wxMpProperties.getAesKey());
        WxMpService service = new WxMpServiceImpl();
        service.setWxMpConfigStorage(config);
        return service;
    }
}
