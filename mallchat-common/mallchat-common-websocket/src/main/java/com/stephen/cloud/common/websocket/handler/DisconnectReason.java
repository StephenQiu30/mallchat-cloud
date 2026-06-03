package com.stephen.cloud.common.websocket.handler;

/**
 * WebSocket 断线原因枚举
 *
 * @author StephenQiu30
 */
public enum DisconnectReason {

    /**
     * 读空闲超时，客户端长时间未发送数据
     */
    TIMEOUT,

    /**
     * 连接发生异常（协议错误、解码失败等）
     */
    EXCEPTION,

    /**
     * 客户端主动关闭连接
     */
    CLIENT_CLOSE,

    /**
     * 服务端主动关闭连接（如未认证、业务拒绝等）
     */
    SERVER_CLOSE
}
