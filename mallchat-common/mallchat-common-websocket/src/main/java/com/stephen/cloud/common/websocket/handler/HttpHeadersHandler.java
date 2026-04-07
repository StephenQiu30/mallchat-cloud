package com.stephen.cloud.common.websocket.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.HashMap;
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

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            
            // 1. 获取 Token (优先从 Header 获取，其次从 Query Param 获取)
            String token = request.headers().get("Authorization");
            if (StrUtil.isBlank(token)) {
                // 尝试从 URL 参数中获取 token
                String uri = request.uri();
                Map<String, String> queryParams = parseQueryParams(uri);
                token = queryParams.get("token");
            }

            // 2. 身份认证
            if (StrUtil.isNotBlank(token)) {
                try {
                    // 去掉 "Bearer " 前缀 if present
                    if (token.startsWith("Bearer ")) {
                        token = token.substring(7);
                    }
                    
                    Object loginId = StpUtil.getLoginIdByToken(token);
                    if (loginId != null) {
                        String userId = String.valueOf(loginId);
                        // 将 userId 绑定到 Channel
                        ctx.channel().attr(ATTR_USER_ID).set(userId);
                        log.info("WebSocket 握手认证成功，用户ID: {}", userId);
                    } else {
                        log.warn("WebSocket 握手认证失败：Token 无效");
                        // 认证失败不一定要立刻断开，某些业务可能允许匿名连接，
                        // 但对于 IM 系统，通常建议直接拒绝。
                        // sendError(ctx, HttpResponseStatus.UNAUTHORIZED);
                        // return;
                    }
                } catch (Exception e) {
                    log.error("WebSocket 握手认证异常", e);
                }
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
}
