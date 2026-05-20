package com.stephen.cloud.gateway.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

class GatewayRateLimitConfigTest {

    @Test
    void shouldApplyStricterUserRateLimitToChatMessageSendRoute() {
        Map<String, Object> config = loadApplicationConfig();
        List<Map<String, Object>> routes = routes(config);

        Map<String, Object> route = routes.stream()
                .filter(item -> "mallchat-chat-message-send-service".equals(item.get("id")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少消息发送专用限流路由"));

        Assertions.assertEquals(List.of("Path=/api/chat/message/send"), route.get("predicates"));
        List<Map<String, Object>> filters = (List<Map<String, Object>>) route.get("filters");
        Map<String, Object> rateLimiter = filters.stream()
                .filter(item -> "RequestRateLimiter".equals(item.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少 RequestRateLimiter"));
        Map<String, Object> args = (Map<String, Object>) rateLimiter.get("args");
        Assertions.assertEquals(5, args.get("redis-rate-limiter.replenishRate"));
        Assertions.assertEquals(10, args.get("redis-rate-limiter.burstCapacity"));
        Assertions.assertEquals("#{@userKeyResolver}", args.get("key-resolver"));
    }

    @Test
    void shouldKeepGeneralChatRouteAfterSpecificMessageSendRoute() {
        Map<String, Object> config = loadApplicationConfig();
        List<Map<String, Object>> routes = routes(config);

        int sendRouteIndex = routeIndex(routes, "mallchat-chat-message-send-service");
        int chatRouteIndex = routeIndex(routes, "mallchat-chat-service");

        Assertions.assertTrue(sendRouteIndex >= 0, "缺少消息发送专用路由");
        Assertions.assertTrue(chatRouteIndex >= 0, "缺少聊天通用路由");
        Assertions.assertTrue(sendRouteIndex < chatRouteIndex, "消息发送路由需要优先于聊天通用路由");
    }

    private Map<String, Object> loadApplicationConfig() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.yml");
        Assertions.assertNotNull(inputStream, "application.yml 不存在");
        return new Yaml().load(inputStream);
    }

    private List<Map<String, Object>> routes(Map<String, Object> config) {
        Map<String, Object> spring = (Map<String, Object>) config.get("spring");
        Map<String, Object> cloud = (Map<String, Object>) spring.get("cloud");
        Map<String, Object> gateway = (Map<String, Object>) cloud.get("gateway");
        Map<String, Object> server = (Map<String, Object>) gateway.get("server");
        Map<String, Object> webflux = (Map<String, Object>) server.get("webflux");
        return (List<Map<String, Object>>) webflux.get("routes");
    }

    private int routeIndex(List<Map<String, Object>> routes, String routeId) {
        for (int i = 0; i < routes.size(); i++) {
            if (routeId.equals(routes.get(i).get("id"))) {
                return i;
            }
        }
        return -1;
    }
}
