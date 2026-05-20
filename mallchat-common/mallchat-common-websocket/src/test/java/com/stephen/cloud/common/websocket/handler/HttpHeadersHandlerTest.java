package com.stephen.cloud.common.websocket.handler;

import cn.dev33.satoken.stp.StpUtil;
import com.stephen.cloud.common.websocket.config.WebSocketProperties;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

class HttpHeadersHandlerTest {

    @Test
    void shouldRejectHandshakeWhenTokenMissing() {
        EmbeddedChannel channel = new EmbeddedChannel(new HttpHeadersHandler());
        FullHttpRequest request = newRequest("/websocket");

        Assertions.assertFalse(channel.writeInbound(request));

        FullHttpResponse response = channel.readOutbound();
        Assertions.assertEquals(HttpResponseStatus.UNAUTHORIZED, response.status());
        Assertions.assertNull(channel.attr(HttpHeadersHandler.ATTR_USER_ID).get());
        response.release();
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldRejectHandshakeWhenTokenInvalid() {
        EmbeddedChannel channel = new EmbeddedChannel(new HttpHeadersHandler());
        FullHttpRequest request = newRequest("/websocket?token=bad-token");

        try (MockedStatic<StpUtil> stpUtil = Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken("bad-token")).thenReturn(null);

            Assertions.assertFalse(channel.writeInbound(request));
        }

        FullHttpResponse response = channel.readOutbound();
        Assertions.assertEquals(HttpResponseStatus.UNAUTHORIZED, response.status());
        Assertions.assertNull(channel.attr(HttpHeadersHandler.ATTR_USER_ID).get());
        response.release();
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldBindUserIdAndForwardHandshakeWhenTokenValid() {
        EmbeddedChannel channel = new EmbeddedChannel(new HttpHeadersHandler());
        FullHttpRequest request = newRequest("/websocket");
        request.headers().set(HttpHeaderNames.AUTHORIZATION, "Bearer valid-token");

        try (MockedStatic<StpUtil> stpUtil = Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken("valid-token")).thenReturn(1001L);

            Assertions.assertTrue(channel.writeInbound(request));
        }

        FullHttpRequest forwarded = channel.readInbound();
        Assertions.assertSame(request, forwarded);
        Assertions.assertEquals("1001", channel.attr(HttpHeadersHandler.ATTR_USER_ID).get());
        forwarded.release();
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldBindUserIdAndForwardHandshakeWhenQueryTokenValid() {
        EmbeddedChannel channel = new EmbeddedChannel(new HttpHeadersHandler());
        FullHttpRequest request = newRequest("/websocket?token=query-token");

        try (MockedStatic<StpUtil> stpUtil = Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken("query-token")).thenReturn(1002L);

            Assertions.assertTrue(channel.writeInbound(request));
        }

        FullHttpRequest forwarded = channel.readInbound();
        Assertions.assertSame(request, forwarded);
        Assertions.assertEquals("1002", channel.attr(HttpHeadersHandler.ATTR_USER_ID).get());
        forwarded.release();
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldRejectHandshakeWhenOriginIsNotAllowed() {
        WebSocketProperties properties = new WebSocketProperties();
        properties.setAllowedOrigins(List.of("https://chat.example.com"));
        EmbeddedChannel channel = new EmbeddedChannel(new HttpHeadersHandler(properties));
        FullHttpRequest request = newRequest("/websocket?token=valid-token");
        request.headers().set(HttpHeaderNames.ORIGIN, "https://evil.example.com");

        Assertions.assertFalse(channel.writeInbound(request));

        FullHttpResponse response = channel.readOutbound();
        Assertions.assertEquals(HttpResponseStatus.FORBIDDEN, response.status());
        Assertions.assertNull(channel.attr(HttpHeadersHandler.ATTR_USER_ID).get());
        response.release();
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldRejectHandshakeWhenOriginMissingAndAllowlistConfigured() {
        WebSocketProperties properties = new WebSocketProperties();
        properties.setAllowedOrigins(List.of("https://chat.example.com"));
        EmbeddedChannel channel = new EmbeddedChannel(new HttpHeadersHandler(properties));
        FullHttpRequest request = newRequest("/websocket?token=valid-token");

        Assertions.assertFalse(channel.writeInbound(request));

        FullHttpResponse response = channel.readOutbound();
        Assertions.assertEquals(HttpResponseStatus.FORBIDDEN, response.status());
        Assertions.assertNull(channel.attr(HttpHeadersHandler.ATTR_USER_ID).get());
        response.release();
        channel.finishAndReleaseAll();
    }

    private FullHttpRequest newRequest(String uri) {
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
    }
}
