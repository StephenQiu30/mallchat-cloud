package com.stephen.cloud.common.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WebSocket 配置属性
 *
 * @author StephenQiu30
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "websocket")
public class WebSocketProperties {

    /**
     * 是否启动 WebSocket 服务器
     */
    private Boolean enabled = false;

    /**
     * WebSocket 端口
     */
    private Integer port = 9090;

    /**
     * Boss 线程数
     */
    private Integer bossThread = 1;

    /**
     * Worker 线程数
     */
    private Integer workerThread = 4;

    /**
     * WebSocket 路径
     */
    private String path = "/websocket";

    /**
     * 允许握手的 Origin 列表，为空时不限制
     */
    private List<String> allowedOrigins = List.of();

    /**
     * 单用户本地最大连接数
     */
    private Integer maxConnectionsPerUser = 5;

    /**
     * 同用户最小连接间隔，0 表示不限制
     */
    private Long minConnectIntervalMillis = 0L;

    /**
     * 读空闲超时时间（秒），即多长时间没有读取到数据就触发读空闲事件
     */
    private Long heartbeatReaderIdle = 60L;

    /**
     * 写空闲超时时间（秒），即多长时间没有写数据就触发写空闲事件（服务器主动心跳）
     */
    private Long heartbeatWriterIdle = 30L;

}
