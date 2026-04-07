package com.stephen.cloud.api.chat.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息状态枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum MessageStatusEnum {

    /**
     * 正常
     */
    NORMAL(0, "正常"),

    /**
     * 已撤回
     */
    RECALL(1, "已撤回"),

    /**
     * 已删除
     */
    DELETE(2, "已删除");

    /**
     * 编码
     */
    private final Integer code;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 获取值列表
     *
     * @return {@link List<Integer>}
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.code).collect(Collectors.toList());
    }

    /**
     * 根据 code 获取枚举
     *
     * @param code code
     * @return {@link MessageStatusEnum}
     */
    public static MessageStatusEnum getEnumByCode(Integer code) {
        if (ObjectUtils.isEmpty(code)) {
            return null;
        }
        for (MessageStatusEnum statusEnum : MessageStatusEnum.values()) {
            if (statusEnum.code.equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}
