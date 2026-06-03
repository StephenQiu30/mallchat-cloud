package com.stephen.cloud.gateway.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

class GatewayRateLimitBehaviorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/chat/message/send",
            "/api/chat/friend/apply/add",
            "/api/chat/moment/publish",
            "/api/moments",
            "/api/file/upload"
    })
    void shouldContinueRequestWhenRateLimiterAllowsCoreWriteApi(String path) {
        MockServerWebExchange exchange = exchange(path);
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        GatewayFilter filter = rateLimitFilter(true);

        filter.filter(exchange, next -> {
            chainInvoked.set(true);
            next.getResponse().setStatusCode(HttpStatus.OK);
            return next.getResponse().setComplete();
        }).block();

        Assertions.assertTrue(chainInvoked.get());
        Assertions.assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/chat/message/send",
            "/api/chat/friend/apply/add",
            "/api/chat/moment/publish",
            "/api/moments",
            "/api/file/upload"
    })
    void shouldReturnTooManyRequestsWhenRateLimiterRejectsCoreWriteApi(String path) {
        MockServerWebExchange exchange = exchange(path);
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        GatewayFilter filter = rateLimitFilter(false);

        filter.filter(exchange, next -> {
            chainInvoked.set(true);
            return next.getResponse().setComplete();
        }).block();

        Assertions.assertFalse(chainInvoked.get());
        Assertions.assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
    }

    private GatewayFilter rateLimitFilter(boolean allowed) {
        KeyResolver keyResolver = exchange -> Mono.just("1001");
        RateLimiter<RequestRateLimiterGatewayFilterFactory.Config> rateLimiter = new FakeRateLimiter(allowed);
        RequestRateLimiterGatewayFilterFactory factory = new RequestRateLimiterGatewayFilterFactory(rateLimiter, keyResolver);
        RequestRateLimiterGatewayFilterFactory.Config config = new RequestRateLimiterGatewayFilterFactory.Config()
                .setKeyResolver(keyResolver)
                .setRateLimiter(rateLimiter)
                .setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        config.setRouteId("mallchat-chat-message-send-service");
        return factory.apply(config);
    }

    private MockServerWebExchange exchange(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.post(path).header("userId", "1001").build());
    }

    private static class FakeRateLimiter implements RateLimiter<RequestRateLimiterGatewayFilterFactory.Config> {

        private final boolean allowed;

        private FakeRateLimiter(boolean allowed) {
            this.allowed = allowed;
        }

        @Override
        public Mono<Response> isAllowed(String routeId, String id) {
            return Mono.just(new Response(allowed, Map.of("X-RateLimit-Remaining", allowed ? "1" : "0")));
        }

        @Override
        public Map<String, RequestRateLimiterGatewayFilterFactory.Config> getConfig() {
            return new HashMap<>();
        }

        @Override
        public Class<RequestRateLimiterGatewayFilterFactory.Config> getConfigClass() {
            return RequestRateLimiterGatewayFilterFactory.Config.class;
        }

        @Override
        public RequestRateLimiterGatewayFilterFactory.Config newConfig() {
            return new RequestRateLimiterGatewayFilterFactory.Config();
        }
    }
}
