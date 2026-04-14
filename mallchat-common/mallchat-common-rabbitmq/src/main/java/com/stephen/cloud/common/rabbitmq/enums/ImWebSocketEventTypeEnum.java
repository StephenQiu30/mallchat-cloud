package com.stephen.cloud.common.rabbitmq.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * IM 业务 WebSocket 事件类型
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum ImWebSocketEventTypeEnum {

    CHAT_MESSAGE("CHAT_MESSAGE", "消息下发"),
    MESSAGE_RECALL("MESSAGE_RECALL", "消息撤回"),
    MESSAGE_READ("MESSAGE_READ", "消息已读"),
    SESSION_UPDATE("SESSION_UPDATE", "会话更新"),
    FRIEND_APPLY("FRIEND_APPLY", "好友申请"),
    FRIEND_APPROVE("FRIEND_APPROVE", "好友申请通过"),
    ONLINE_STATUS("ONLINE_STATUS", "在线状态");

    private final String code;

    private final String desc;

    public static ImWebSocketEventTypeEnum getEnumByCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (ImWebSocketEventTypeEnum typeEnum : values()) {
            if (typeEnum.code.equalsIgnoreCase(code)) {
                return typeEnum;
            }
        }
        return null;
    }
}
