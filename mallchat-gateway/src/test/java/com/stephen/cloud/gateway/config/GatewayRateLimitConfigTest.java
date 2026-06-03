package com.stephen.cloud.gateway.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

class GatewayRateLimitConfigTest {

    @Test
    void shouldApplyStricterUserRateLimitToCoreWriteRoutes() {
        Map<String, Object> config = loadApplicationConfig();
        List<Map<String, Object>> routes = routes(config);

        Map<String, String> expectedRoutes = Map.of(
                "mallchat-chat-message-send-service", "Path=/api/chat/message/send",
                "mallchat-chat-friend-apply-service", "Path=/api/chat/friend/apply/add",
                "mallchat-chat-moment-publish-service", "Path=/api/chat/moment/publish",
                "mallchat-moment-create-service", "Path=/api/moments",
                "mallchat-file-upload-service", "Path=/api/file/upload"
        );

        expectedRoutes.forEach((routeId, path) -> {
            Map<String, Object> route = findRoute(routes, routeId);
            Assertions.assertEquals(List.of(path), route.get("predicates"));
            Map<String, Object> args = rateLimiterArgs(route);
            Assertions.assertEquals(5, args.get("redis-rate-limiter.replenishRate"));
            Assertions.assertEquals(10, args.get("redis-rate-limiter.burstCapacity"));
            Assertions.assertEquals("#{@userKeyResolver}", args.get("key-resolver"));
        });
    }

    @Test
    void shouldKeepGeneralChatRouteAfterSpecificMessageSendRoute() {
        Map<String, Object> config = loadApplicationConfig();
        List<Map<String, Object>> routes = routes(config);

        int sendRouteIndex = routeIndex(routes, "mallchat-chat-message-send-service");
        int friendApplyRouteIndex = routeIndex(routes, "mallchat-chat-friend-apply-service");
        int momentPublishRouteIndex = routeIndex(routes, "mallchat-chat-moment-publish-service");
        int momentCreateRouteIndex = routeIndex(routes, "mallchat-moment-create-service");
        int chatRouteIndex = routeIndex(routes, "mallchat-chat-service");
        int fileUploadRouteIndex = routeIndex(routes, "mallchat-file-upload-service");
        int fileRouteIndex = routeIndex(routes, "mallchat-file-service");

        Assertions.assertTrue(sendRouteIndex >= 0, "缺少消息发送专用路由");
        Assertions.assertTrue(friendApplyRouteIndex >= 0, "缺少好友申请专用路由");
        Assertions.assertTrue(momentPublishRouteIndex >= 0, "缺少动态发布专用路由");
        Assertions.assertTrue(momentCreateRouteIndex >= 0, "缺少动态创建专用路由");
        Assertions.assertTrue(chatRouteIndex >= 0, "缺少聊天通用路由");
        Assertions.assertTrue(fileUploadRouteIndex >= 0, "缺少文件上传专用路由");
        Assertions.assertTrue(fileRouteIndex >= 0, "缺少文件通用路由");
        Assertions.assertTrue(sendRouteIndex < chatRouteIndex, "消息发送路由需要优先于聊天通用路由");
        Assertions.assertTrue(friendApplyRouteIndex < chatRouteIndex, "好友申请路由需要优先于聊天通用路由");
        Assertions.assertTrue(momentPublishRouteIndex < chatRouteIndex, "动态发布路由需要优先于聊天通用路由");
        Assertions.assertTrue(fileUploadRouteIndex < fileRouteIndex, "文件上传路由需要优先于文件通用路由");
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

    private Map<String, Object> findRoute(List<Map<String, Object>> routes, String routeId) {
        return routes.stream()
                .filter(item -> routeId.equals(item.get("id")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少核心写接口专用限流路由: " + routeId));
    }

    private Map<String, Object> rateLimiterArgs(Map<String, Object> route) {
        List<Map<String, Object>> filters = (List<Map<String, Object>>) route.get("filters");
        Map<String, Object> rateLimiter = filters.stream()
                .filter(item -> "RequestRateLimiter".equals(item.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少 RequestRateLimiter"));
        return (Map<String, Object>) rateLimiter.get("args");
    }
}
