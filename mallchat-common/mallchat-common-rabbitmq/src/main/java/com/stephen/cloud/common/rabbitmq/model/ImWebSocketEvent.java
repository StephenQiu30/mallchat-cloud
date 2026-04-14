package com.stephen.cloud.common.rabbitmq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * IM 业务 WebSocket 事件包装
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImWebSocketEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 事件类型
     */
    private String type;

    /**
     * 业务幂等ID
     */
    private String bizId;

    /**
     * 房间ID
     */
    private Long roomId;

    /**
     * 事件负载
     */
    private Object data;
}
