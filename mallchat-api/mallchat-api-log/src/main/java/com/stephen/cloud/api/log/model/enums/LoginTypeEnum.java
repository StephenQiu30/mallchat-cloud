package com.stephen.cloud.api.log.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录类型枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum LoginTypeEnum {

    /**
     * 邮箱登录
     */
    EMAIL("邮箱登录", "EMAIL"),

    /**
     * 微信小程序登录
     */
    WECHAT_MA("微信小程序登录", "WECHAT_MA"),

    /**
     * 微信 App 登录
     */
    WECHAT_APP("微信 App 登录", "WECHAT_APP"),

    /**
     * Apple 登录
     */
    APPLE("Apple 登录", "APPLE");

    /**
     * 登录类型文案
     */
    private final String text;

    /**
     * 登录类型值
     */
    private final String value;

    /**
     * 获取值列表
     *
     * @return List<String>
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value value
     * @return {@link LoginTypeEnum}
     */
    public static LoginTypeEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (LoginTypeEnum anEnum : LoginTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

}
