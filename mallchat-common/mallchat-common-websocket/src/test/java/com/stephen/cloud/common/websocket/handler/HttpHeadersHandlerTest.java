package com.stephen.cloud.common.websocket.handler;

import cn.dev33.satoken.stp.StpUtil;
import com.stephen.cloud.common.websocket.config.WebSocketProperties;
import io.netty.buffer.ByteBuf;
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

import java.nio.charset.StandardCharsets;
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

    @Test
    void shouldReflectAllowedOriginsChangeDynamically() {
        WebSocketProperties properties = new WebSocketProperties();
        properties.setAllowedOrigins(List.of("https://chat.example.com"));

        // 初始配置：evil origin 被拒绝（新连接）
        EmbeddedChannel channel1 = new EmbeddedChannel(new HttpHeadersHandler(properties));
        FullHttpRequest request1 = newRequest("/websocket?token=valid-token");
        request1.headers().set(HttpHeaderNames.ORIGIN, "https://evil.example.com");
        Assertions.assertFalse(channel1.writeInbound(request1));
        FullHttpResponse response1 = channel1.readOutbound();
        Assertions.assertEquals(HttpResponseStatus.FORBIDDEN, response1.status());
        response1.release();
        channel1.finishAndReleaseAll();

        // 动态更新 allowedOrigins，包含之前的 evil origin
        properties.setAllowedOrigins(List.of("https://evil.example.com"));

        // 更新后：新连接使用新配置，evil origin 应该被允许
        EmbeddedChannel channel2 = new EmbeddedChannel(new HttpHeadersHandler(properties));
        FullHttpRequest request2 = newRequest("/websocket?token=valid-token");
        request2.headers().set(HttpHeaderNames.ORIGIN, "https://evil.example.com");
        try (MockedStatic<StpUtil> stpUtil = Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken("valid-token")).thenReturn(2001L);
            Assertions.assertTrue(channel2.writeInbound(request2));
        }
        FullHttpRequest forwarded = channel2.readInbound();
        Assertions.assertSame(request2, forwarded);
        Assertions.assertEquals("2001", channel2.attr(HttpHeadersHandler.ATTR_USER_ID).get());
        forwarded.release();
        channel2.finishAndReleaseAll();
    }

    @Test
    void shouldAllowAllOriginsWhenAllowedOriginsClearedDynamically() {
        WebSocketProperties properties = new WebSocketProperties();
        properties.setAllowedOrigins(List.of("https://chat.example.com"));

        // 初始配置：evil origin 被拒绝（新连接）
        EmbeddedChannel channel1 = new EmbeddedChannel(new HttpHeadersHandler(properties));
        FullHttpRequest request1 = newRequest("/websocket?token=valid-token");
        request1.headers().set(HttpHeaderNames.ORIGIN, "https://evil.example.com");
        Assertions.assertFalse(channel1.writeInbound(request1));
        FullHttpResponse response1 = channel1.readOutbound();
        Assertions.assertEquals(HttpResponseStatus.FORBIDDEN, response1.status());
        response1.release();
        channel1.finishAndReleaseAll();

        // 动态清空 allowedOrigins，表示不限制
        properties.setAllowedOrigins(List.of());

        // 清空后：新连接不限制，任何 origin 都应被允许
        EmbeddedChannel channel2 = new EmbeddedChannel(new HttpHeadersHandler(properties));
        FullHttpRequest request2 = newRequest("/websocket?token=valid-token");
        request2.headers().set(HttpHeaderNames.ORIGIN, "https://evil.example.com");
        try (MockedStatic<StpUtil> stpUtil = Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken("valid-token")).thenReturn(2002L);
            Assertions.assertTrue(channel2.writeInbound(request2));
        }
        FullHttpRequest forwarded = channel2.readInbound();
        Assertions.assertSame(request2, forwarded);
        Assertions.assertEquals("2002", channel2.attr(HttpHeadersHandler.ATTR_USER_ID).get());
        forwarded.release();
        channel2.finishAndReleaseAll();
    }

    @Test
    void shouldRestrictOriginsWhenAllowedOriginsPopulatedDynamically() {
        WebSocketProperties properties = new WebSocketProperties();
        // 初始为空，不限制
        EmbeddedChannel channel1 = new EmbeddedChannel(new HttpHeadersHandler(properties));

        FullHttpRequest request1 = newRequest("/websocket?token=valid-token");
        request1.headers().set(HttpHeaderNames.ORIGIN, "https://any.example.com");
        try (MockedStatic<StpUtil> stpUtil = Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken("valid-token")).thenReturn(2003L);
            Assertions.assertTrue(channel1.writeInbound(request1));
        }
        FullHttpRequest forwarded1 = channel1.readInbound();
        Assertions.assertSame(request1, forwarded1);
        Assertions.assertEquals("2003", channel1.attr(HttpHeadersHandler.ATTR_USER_ID).get());
        forwarded1.release();
        channel1.finishAndReleaseAll();

        // 动态填充 allowedOrigins
        properties.setAllowedOrigins(List.of("https://trusted.example.com"));

        // 填充后：新连接使用新配置，不在列表中的 origin 应被拒绝
        EmbeddedChannel channel2 = new EmbeddedChannel(new HttpHeadersHandler(properties));
        FullHttpRequest request2 = newRequest("/websocket?token=valid-token");
        request2.headers().set(HttpHeaderNames.ORIGIN, "https://any.example.com");
        Assertions.assertFalse(channel2.writeInbound(request2));
        FullHttpResponse response2 = channel2.readOutbound();
        Assertions.assertEquals(HttpResponseStatus.FORBIDDEN, response2.status());
        response2.release();
        channel2.finishAndReleaseAll();
    }

    @Test
    void shouldRejectHandshakeWhenTokenExpired() {
        EmbeddedChannel channel = new EmbeddedChannel(new HttpHeadersHandler());
        FullHttpRequest request = newRequest("/websocket");
        request.headers().set(HttpHeaderNames.AUTHORIZATION, "Bearer expired-token");

        try (MockedStatic<StpUtil> stpUtil = Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken("expired-token")).thenReturn(null);

            Assertions.assertFalse(channel.writeInbound(request));
        }

        FullHttpResponse response = channel.readOutbound();
        Assertions.assertEquals(HttpResponseStatus.UNAUTHORIZED, response.status());
        ByteBuf content = response.content();
        String body = content.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(body.contains("过期"), "响应体应包含过期提示，实际: " + body);
        Assertions.assertNull(channel.attr(HttpHeadersHandler.ATTR_USER_ID).get());
        response.release();
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldRejectHandshakeWhenTokenKickedByNewDevice() {
        EmbeddedChannel channel = new EmbeddedChannel(new HttpHeadersHandler());
        FullHttpRequest request = newRequest("/websocket");
        request.headers().set(HttpHeaderNames.AUTHORIZATION, "Bearer kicked-token");

        try (MockedStatic<StpUtil> stpUtil = Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken("kicked-token")).thenReturn(null);

            Assertions.assertFalse(channel.writeInbound(request));
        }

        FullHttpResponse response = channel.readOutbound();
        Assertions.assertEquals(HttpResponseStatus.UNAUTHORIZED, response.status());
        ByteBuf content = response.content();
        String body = content.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(body.contains("过期"), "响应体应包含过期提示，实际: " + body);
        Assertions.assertNull(channel.attr(HttpHeadersHandler.ATTR_USER_ID).get());
        response.release();
        channel.finishAndReleaseAll();
    }

    private FullHttpRequest newRequest(String uri) {
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
    }
}
