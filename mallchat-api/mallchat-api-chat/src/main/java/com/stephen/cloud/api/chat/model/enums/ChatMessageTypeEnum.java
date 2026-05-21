package com.stephen.cloud.api.chat.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天消息类型枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum ChatMessageTypeEnum {

    TEXT(1, "文本"),
    IMAGE(2, "图片"),
    FILE(3, "文件"),
    VOICE(4, "语音"),
    VIDEO(5, "视频"),
    STICKER(6, "表情");

    private final Integer code;

    private final String desc;

    public static ChatMessageTypeEnum getEnumByCode(Integer code) {
        if (ObjectUtils.isEmpty(code)) {
            return null;
        }
        for (ChatMessageTypeEnum typeEnum : values()) {
            if (typeEnum.code.equals(code)) {
                return typeEnum;
            }
        }
        return null;
    }

    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(ChatMessageTypeEnum::getCode).collect(Collectors.toList());
    }
}
