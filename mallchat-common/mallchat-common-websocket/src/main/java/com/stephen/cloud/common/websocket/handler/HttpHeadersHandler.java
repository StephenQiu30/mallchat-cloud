package com.stephen.cloud.common.websocket.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.stephen.cloud.common.websocket.config.WebSocketProperties;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP 请求头处理器
 * 用于在 WebSocket 握手前拦截 HTTP 请求，进行身份认证
 *
 * @author StephenQiu30
 */
@Slf4j
public class HttpHeadersHandler extends ChannelInboundHandlerAdapter {

    public static final AttributeKey<String> ATTR_USER_ID = AttributeKey.valueOf("ws_user_id");

    private final WebSocketProperties webSocketProperties;

    public HttpHeadersHandler() {
        this(new WebSocketProperties());
    }

    public HttpHeadersHandler(WebSocketProperties webSocketProperties) {
        this.webSocketProperties = webSocketProperties == null ? new WebSocketProperties() : webSocketProperties;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;

            if (!isAllowedOrigin(request)) {
                reject(ctx, request, HttpResponseStatus.FORBIDDEN, "WebSocket 握手拒绝：Origin 不在允许列表内");
                return;
            }

            // 1. 获取 Token (优先从 Header 获取，其次从 Query Param 获取)
            String token = request.headers().get("Authorization");
            if (StrUtil.isBlank(token)) {
                // 尝试从 URL 参数中获取 token
                String uri = request.uri();
                Map<String, String> queryParams = parseQueryParams(uri);
                token = queryParams.get("token");
            }

            // 2. 身份认证
            if (StrUtil.isBlank(token)) {
                reject(ctx, request, HttpResponseStatus.UNAUTHORIZED, "WebSocket 握手拒绝：Token 缺失");
                return;
            }

            try {
                // 去掉 "Bearer " 前缀 if present
                if (token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }

                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId == null) {
                    reject(ctx, request, HttpResponseStatus.UNAUTHORIZED, "WebSocket 握手拒绝：Token 无效");
                    return;
                }
                String userId = String.valueOf(loginId);
                // 将 userId 绑定到 Channel
                ctx.channel().attr(ATTR_USER_ID).set(userId);
                log.info("WebSocket 握手认证成功，用户ID: {}", userId);
            } catch (Exception e) {
                log.error("WebSocket 握手认证异常", e);
                reject(ctx, request, HttpResponseStatus.UNAUTHORIZED, "WebSocket 握手拒绝：Token 认证异常");
                return;
            }

            // 握手后就不再需要这个处理器了，移除它
            ctx.pipeline().remove(this);
            // 传递给下一个处理器 (WebSocketServerProtocolHandler)
            ctx.fireChannelRead(msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    /**
     * 解析 URL 中的查询参数
     */
    private Map<String, String> parseQueryParams(String uriStr) {
        Map<String, String> params = new HashMap<>();
        try {
            URI uri = new URI(uriStr);
            String query = uri.getQuery();
            if (StrUtil.isNotBlank(query)) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    if (idx > 0) {
                        params.put(pair.substring(0, idx), pair.substring(idx + 1));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析 URL 参数失败: {}", uriStr);
        }
        return params;
    }

    private boolean isAllowedOrigin(FullHttpRequest request) {
        List<String> allowedOrigins = webSocketProperties.getAllowedOrigins();
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return true;
        }
        String origin = request.headers().get(HttpHeaderNames.ORIGIN);
        if (StrUtil.isBlank(origin)) {
            return false;
        }
        return allowedOrigins.contains("*") || allowedOrigins.contains(origin);
    }

    private void reject(ChannelHandlerContext ctx, FullHttpRequest request, HttpResponseStatus status, String reason) {
        log.warn(reason);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), status);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        ReferenceCountUtil.release(request);
    }
}
