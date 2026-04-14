package com.stephen.cloud.api.chat.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 聊天室成员角色枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum ChatRoomRoleEnum {

    MEMBER(1, "成员"),
    ADMIN(2, "管理员"),
    OWNER(3, "群主");

    private final Integer code;

    private final String desc;

    public static ChatRoomRoleEnum getEnumByCode(Integer code) {
        if (ObjectUtils.isEmpty(code)) {
            return null;
        }
        for (ChatRoomRoleEnum roleEnum : values()) {
            if (roleEnum.code.equals(code)) {
                return roleEnum;
            }
        }
        return null;
    }
}
