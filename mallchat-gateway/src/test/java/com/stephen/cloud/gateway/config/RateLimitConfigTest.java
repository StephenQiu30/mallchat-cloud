package com.stephen.cloud.gateway.config;

import com.stephen.cloud.common.constants.SecurityConstant;
import com.stephen.cloud.gateway.constant.GatewayConstant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class RateLimitConfigTest {

    @Test
    void userKeyResolverShouldPreferAuthenticatedExchangeAttribute() {
        RateLimitConfig config = new RateLimitConfig();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/chat/message/send")
                        .header(SecurityConstant.USER_ID_HEADER, "external-forged")
                        .build());
        exchange.getAttributes().put(GatewayConstant.ATTR_LOGIN_USER_ID, "1001");

        String key = config.userKeyResolver().resolve(exchange).block();

        Assertions.assertEquals("1001", key);
    }
}
