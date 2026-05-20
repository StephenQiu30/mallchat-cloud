package com.stephen.cloud.api.chat.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 举报对象类型枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum ChatReportTargetTypeEnum {

    USER(1, "用户"),
    MESSAGE(2, "消息"),
    MOMENT(3, "动态");

    private final Integer code;

    private final String desc;

    public static ChatReportTargetTypeEnum getEnumByCode(Integer code) {
        if (ObjectUtils.isEmpty(code)) {
            return null;
        }
        for (ChatReportTargetTypeEnum typeEnum : values()) {
            if (typeEnum.code.equals(code)) {
                return typeEnum;
            }
        }
        return null;
    }
}
