package com.stephen.cloud.api.chat.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 聊天室类型枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum ChatRoomTypeEnum {

    GROUP(1, "群聊"),
    PRIVATE(2, "私聊");

    private final Integer code;

    private final String desc;

    public static ChatRoomTypeEnum getEnumByCode(Integer code) {
        if (ObjectUtils.isEmpty(code)) {
            return null;
        }
        for (ChatRoomTypeEnum typeEnum : values()) {
            if (typeEnum.code.equals(code)) {
                return typeEnum;
            }
        }
        return null;
    }
}
