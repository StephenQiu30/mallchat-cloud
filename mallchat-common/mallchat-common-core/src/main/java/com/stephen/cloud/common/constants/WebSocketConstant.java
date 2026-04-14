package com.stephen.cloud.common.constants;

/**
 * WebSocket 相关常量
 *
 * @author StephenQiu30
 */
public interface WebSocketConstant {

    /**
     * 用户连接集合 Key
     */
    String WS_USER_CONNECTIONS_KEY = "ws:user:connections:";

    /**
     * 连接元数据 Key
     */
    String WS_CONNECTION_META_KEY = "ws:connection:";

    /**
     * 连接集合过期时间（秒）
     */
    long WS_CONNECTION_EXPIRE_SECONDS = 86400L;
}
