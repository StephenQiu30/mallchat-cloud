package com.stephen.cloud.gateway.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

class GatewayAuthWhitelistConfigTest {

    @Test
    void authWhitelistShouldOnlyContainPublicReadOrLoginPaths() {
        List<String> whiteList = authWhiteList();

        Assertions.assertFalse(whiteList.contains("/api/user/logout"), "logout 应经过网关统一认证");
        Assertions.assertFalse(whiteList.contains("/api/notification/page"), "通知业务接口不应出现在认证白名单");
    }

    private List<String> authWhiteList() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.yml");
        Assertions.assertNotNull(inputStream, "application.yml 不存在");
        Map<String, Object> config = new Yaml().load(inputStream);
        Map<String, Object> spring = (Map<String, Object>) config.get("spring");
        Map<String, Object> cloud = (Map<String, Object>) spring.get("cloud");
        Map<String, Object> gateway = (Map<String, Object>) cloud.get("gateway");
        Map<String, Object> auth = (Map<String, Object>) gateway.get("auth");
        return (List<String>) auth.get("white-list");
    }
}
