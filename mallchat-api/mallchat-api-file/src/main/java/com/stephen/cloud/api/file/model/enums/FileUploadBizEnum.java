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
    CHAT_FILE("chat_file", "聊天文件"),

    /**
     * 聊天语音
     */
    CHAT_VOICE("chat_voice", "聊天语音"),

    /**
     * 聊天视频
     */
    CHAT_VIDEO("chat_video", "聊天视频");

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
