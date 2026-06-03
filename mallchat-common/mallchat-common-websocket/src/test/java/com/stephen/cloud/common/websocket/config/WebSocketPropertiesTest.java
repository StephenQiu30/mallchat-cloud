package com.stephen.cloud.common.websocket.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

class WebSocketPropertiesTest {

    @Test
    void shouldDisableWebSocketServerByDefault() {
        WebSocketProperties properties = new WebSocketProperties();

        Assertions.assertFalse(properties.getEnabled());
    }

    @Test
    void shouldNotRestrictOriginsByDefault() {
        WebSocketProperties properties = new WebSocketProperties();

        Assertions.assertTrue(properties.getAllowedOrigins().isEmpty());
    }

    @Test
    void shouldUseConservativeRuntimeGuardDefaults() {
        WebSocketProperties properties = new WebSocketProperties();

        Assertions.assertEquals(5, properties.getMaxConnectionsPerUser());
        Assertions.assertEquals(0L, properties.getMinConnectIntervalMillis());
    }

    @Test
    void shouldBeAnnotatedWithRefreshScope() {
        Assertions.assertTrue(
                WebSocketProperties.class.isAnnotationPresent(RefreshScope.class),
                "WebSocketProperties 应标注 @RefreshScope 以支持配置中心动态刷新"
        );
    }

    @Test
    void shouldUseDefaultHeartbeatParameters() {
        WebSocketProperties properties = new WebSocketProperties();

        Assertions.assertEquals(60L, properties.getHeartbeatReaderIdle());
        Assertions.assertEquals(30L, properties.getHeartbeatWriterIdle());
    }

    @Test
    void shouldAcceptCustomHeartbeatParameters() {
        WebSocketProperties properties = new WebSocketProperties();
        properties.setHeartbeatReaderIdle(120L);
        properties.setHeartbeatWriterIdle(60L);

        Assertions.assertEquals(120L, properties.getHeartbeatReaderIdle());
        Assertions.assertEquals(60L, properties.getHeartbeatWriterIdle());
    }

    @SpringJUnitConfig(RefreshScopeTestConfig.class)
    static class RefreshScopeProxyTest {

        @Autowired
        private ApplicationContext context;

        @Test
        void shouldCreateScopedProxyForWebSocketProperties() {
            Object bean = context.getBean("webSocketProperties");

            // @RefreshScope 创建 CGLIB 代理
            Assertions.assertTrue(
                    AopUtils.isAopProxy(bean),
                    "WebSocketProperties 应为 @RefreshScope 创建的 ScopedProxy"
            );

            // 代理仍能正确读取属性
            WebSocketProperties properties = context.getBean(WebSocketProperties.class);
            Assertions.assertNotNull(properties.getAllowedOrigins());
            Assertions.assertTrue(properties.getAllowedOrigins().isEmpty());
        }
    }

    @Configuration
    static class RefreshScopeTestConfig {

        @Bean
        @RefreshScope
        public WebSocketProperties webSocketProperties() {
            return new WebSocketProperties();
        }
    }
}
