package com.stephen.cloud.api.chat.model.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 聊天消息类型枚举测试
 *
 * @author StephenQiu30
 */
class ChatMessageTypeEnumTest {

    @ParameterizedTest
    @CsvSource({
            "1, TEXT, 文本",
            "2, IMAGE, 图片",
            "3, FILE, 文件",
            "4, VOICE, 语音",
            "5, VIDEO, 视频",
            "6, STICKER, 表情"
    })
    void shouldContainAllMessageTypes(int code, String name, String desc) {
        ChatMessageTypeEnum type = ChatMessageTypeEnum.getEnumByCode(code);
        assertNotNull(type, "枚举值 " + code + " 应存在");
        assertEquals(name, type.name());
        assertEquals(desc, type.getDesc());
    }

    @Test
    void shouldReturnNullForUnknownCode() {
        assertNull(ChatMessageTypeEnum.getEnumByCode(99));
    }

    @Test
    void shouldReturnNullForNullCode() {
        assertNull(ChatMessageTypeEnum.getEnumByCode(null));
    }

    @Test
    void shouldReturnAllValues() {
        assertEquals(6, ChatMessageTypeEnum.getValues().size());
    }
}
