package com.stephen.cloud.api.chat.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息投递状态枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum MessageDeliveryStatusEnum {

    /**
     * 待投递
     */
    PENDING(0, "待投递"),

    /**
     * 已投递
     */
    DELIVERED(1, "已投递"),

    /**
     * 投递失败
     */
    FAILED(2, "投递失败");

    /**
     * 编码
     */
    private final Integer code;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 根据 code 获取枚举
     *
     * @param code code
     * @return {@link MessageDeliveryStatusEnum}
     */
    public static MessageDeliveryStatusEnum getEnumByCode(Integer code) {
        if (ObjectUtils.isEmpty(code)) {
            return null;
        }
        for (MessageDeliveryStatusEnum statusEnum : values()) {
            if (statusEnum.code.equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }

    /**
     * 获取值列表
     *
     * @return {@link List<Integer>}
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.code).collect(Collectors.toList());
    }
}
