package com.stephen.cloud.api.log.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录状态枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum LoginStatusEnum {

    /**
     * 登录成功
     */
    SUCCESS("登录成功", "SUCCESS"),

    /**
     * 登录失败
     */
    FAILED("登录失败", "FAILED");

    /**
     * 状态说明
     */
    private final String text;

    /**
     * 状态值
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
     * @return {@link LoginStatusEnum}
     */
    public static LoginStatusEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (LoginStatusEnum anEnum : LoginStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

}
