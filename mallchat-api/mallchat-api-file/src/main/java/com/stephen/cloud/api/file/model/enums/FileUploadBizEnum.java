package com.stephen.cloud.api.file.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务类型枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum FileUploadBizEnum {

    /**
     * 用户头像
     */
    USER_AVATAR("user_avatar", "用户头像"),

    /**
     * 聊天图片
     */
    CHAT_IMAGE("chat_image", "聊天图片"),

    /**
     * 聊天文件
     */
    CHAT_FILE("chat_file", "聊天文件");

    private final String code;
    private final String desc;

    public static FileUploadBizEnum getEnumByCode(String code) {
        for (FileUploadBizEnum bizTypeEnum : values()) {
            if (bizTypeEnum.code.equals(code)) {
                return bizTypeEnum;
            }
        }
        return null;
    }
}
