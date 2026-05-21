package com.stephen.cloud.chat.support;

import com.stephen.cloud.api.chat.model.enums.ChatMessageTypeEnum;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ChatMessageHelperTest {

    @Test
    void shouldAcceptImageMessageWithRequiredExtraFields() {
        ChatMessage chatMessage = createMessage(ChatMessageTypeEnum.IMAGE,
                "{\"url\":\"https://example.com/a.png\",\"width\":100,\"height\":200,\"size\":4096}");

        Assertions.assertDoesNotThrow(() -> ChatMessageHelper.validate(chatMessage));
    }

    @Test
    void shouldAcceptImageMessageWithNumericStringFields() {
        ChatMessage chatMessage = createMessage(ChatMessageTypeEnum.IMAGE,
                "{\"url\":\"https://example.com/a.png\",\"width\":\"100\",\"height\":\"200\",\"size\":\"4096\"}");

        Assertions.assertDoesNotThrow(() -> ChatMessageHelper.validate(chatMessage));
    }

    @Test
    void shouldRejectImageMessageWithBlankUrl() {
        ChatMessage chatMessage = createMessage(ChatMessageTypeEnum.IMAGE,
                "{\"url\":\" \",\"width\":100,\"height\":200,\"size\":4096}");

        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(chatMessage));
    }

    @Test
    void shouldRejectImageMessageWithInvalidNumberFields() {
        ChatMessage zeroWidth = createMessage(ChatMessageTypeEnum.IMAGE,
                "{\"url\":\"https://example.com/a.png\",\"width\":0,\"height\":200,\"size\":4096}");
        ChatMessage negativeSize = createMessage(ChatMessageTypeEnum.IMAGE,
                "{\"url\":\"https://example.com/a.png\",\"width\":100,\"height\":200,\"size\":-1}");
        ChatMessage nonNumericHeight = createMessage(ChatMessageTypeEnum.IMAGE,
                "{\"url\":\"https://example.com/a.png\",\"width\":100,\"height\":\"bad\",\"size\":4096}");

        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(zeroWidth));
        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(negativeSize));
        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(nonNumericHeight));
    }

    @Test
    void shouldRejectImageMessageWithMissingNumberFields() {
        ChatMessage missingWidth = createMessage(ChatMessageTypeEnum.IMAGE,
                "{\"url\":\"https://example.com/a.png\",\"height\":200,\"size\":4096}");
        ChatMessage missingHeight = createMessage(ChatMessageTypeEnum.IMAGE,
                "{\"url\":\"https://example.com/a.png\",\"width\":100,\"size\":4096}");
        ChatMessage missingSize = createMessage(ChatMessageTypeEnum.IMAGE,
                "{\"url\":\"https://example.com/a.png\",\"width\":100,\"height\":200}");

        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(missingWidth));
        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(missingHeight));
        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(missingSize));
    }

    @Test
    void shouldAcceptFileMessageWithRequiredExtraFields() {
        ChatMessage chatMessage = createMessage(ChatMessageTypeEnum.FILE,
                "{\"url\":\"https://example.com/a.zip\",\"name\":\"a.zip\",\"size\":1024,\"ext\":\"zip\"}");

        Assertions.assertDoesNotThrow(() -> ChatMessageHelper.validate(chatMessage));
    }

    @Test
    void shouldAcceptVoiceMessageWithRequiredExtraFields() {
        ChatMessage chatMessage = createMessage(ChatMessageTypeEnum.VOICE,
                "{\"url\":\"https://example.com/a.m4a\",\"duration\":12,\"size\":2048,\"format\":\"m4a\"}");

        Assertions.assertDoesNotThrow(() -> ChatMessageHelper.validate(chatMessage));
    }

    @Test
    void shouldRejectVoiceMessageWithoutCompleteExtra() {
        ChatMessage missingFormat = createMessage(ChatMessageTypeEnum.VOICE,
                "{\"url\":\"https://example.com/a.m4a\",\"duration\":12,\"size\":2048}");
        ChatMessage invalidDuration = createMessage(ChatMessageTypeEnum.VOICE,
                "{\"url\":\"https://example.com/a.m4a\",\"duration\":0,\"size\":2048,\"format\":\"m4a\"}");

        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(missingFormat));
        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(invalidDuration));
    }

    @Test
    void shouldAcceptVideoMessageWithRequiredExtraFields() {
        ChatMessage chatMessage = createMessage(ChatMessageTypeEnum.VIDEO,
                "{\"url\":\"https://example.com/a.mp4\",\"duration\":30,\"size\":4096,\"format\":\"mp4\",\"width\":720,\"height\":1280}");

        Assertions.assertDoesNotThrow(() -> ChatMessageHelper.validate(chatMessage));
    }

    @Test
    void shouldRejectVideoMessageWithoutCompleteExtra() {
        ChatMessage missingHeight = createMessage(ChatMessageTypeEnum.VIDEO,
                "{\"url\":\"https://example.com/a.mp4\",\"duration\":30,\"size\":4096,\"format\":\"mp4\",\"width\":720}");
        ChatMessage invalidSize = createMessage(ChatMessageTypeEnum.VIDEO,
                "{\"url\":\"https://example.com/a.mp4\",\"duration\":30,\"size\":\"bad\",\"format\":\"mp4\",\"width\":720,\"height\":1280}");

        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(missingHeight));
        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(invalidSize));
    }

    @Test
    void shouldAcceptStickerMessageWithRequiredExtraFields() {
        ChatMessage chatMessage = createMessage(ChatMessageTypeEnum.STICKER,
                "{\"stickerId\":\"smile-1\",\"name\":\"微笑\",\"url\":\"https://example.com/sticker.png\"}");

        Assertions.assertDoesNotThrow(() -> ChatMessageHelper.validate(chatMessage));
    }

    @Test
    void shouldRejectStickerMessageWithoutRequiredFields() {
        ChatMessage missingStickerId = createMessage(ChatMessageTypeEnum.STICKER,
                "{\"name\":\"微笑\",\"url\":\"https://example.com/sticker.png\"}");
        ChatMessage blankUrl = createMessage(ChatMessageTypeEnum.STICKER,
                "{\"stickerId\":\"smile-1\",\"name\":\"微笑\",\"url\":\" \"}");

        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(missingStickerId));
        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(blankUrl));
    }

    @Test
    void shouldAcceptFileMessageWithNumericStringSize() {
        ChatMessage chatMessage = createMessage(ChatMessageTypeEnum.FILE,
                "{\"url\":\"https://example.com/a.zip\",\"name\":\"a.zip\",\"size\":\"1024\",\"ext\":\"zip\"}");

        Assertions.assertDoesNotThrow(() -> ChatMessageHelper.validate(chatMessage));
    }

    @Test
    void shouldRejectTextMessageWithoutContent() {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setClientMsgId("text-1");
        chatMessage.setType(ChatMessageTypeEnum.TEXT.getCode());

        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(chatMessage));
    }

    @Test
    void shouldRejectFileMessageWithoutCompleteExtra() {
        ChatMessage chatMessage = createMessage(ChatMessageTypeEnum.FILE,
                "{\"url\":\"https://example.com/a.zip\",\"name\":\"a.zip\",\"size\":1024}");

        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(chatMessage));
    }

    @Test
    void shouldRejectFileMessageWithBlankTextFields() {
        ChatMessage blankUrl = createMessage(ChatMessageTypeEnum.FILE,
                "{\"url\":\" \",\"name\":\"a.zip\",\"size\":1024,\"ext\":\"zip\"}");
        ChatMessage blankName = createMessage(ChatMessageTypeEnum.FILE,
                "{\"url\":\"https://example.com/a.zip\",\"name\":\" \",\"size\":1024,\"ext\":\"zip\"}");
        ChatMessage blankExt = createMessage(ChatMessageTypeEnum.FILE,
                "{\"url\":\"https://example.com/a.zip\",\"name\":\"a.zip\",\"size\":1024,\"ext\":\" \"}");

        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(blankUrl));
        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(blankName));
        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(blankExt));
    }

    @Test
    void shouldRejectFileMessageWithMissingSize() {
        ChatMessage chatMessage = createMessage(ChatMessageTypeEnum.FILE,
                "{\"url\":\"https://example.com/a.zip\",\"name\":\"a.zip\",\"ext\":\"zip\"}");

        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(chatMessage));
    }

    @Test
    void shouldRejectFileMessageWithInvalidSize() {
        ChatMessage zeroSize = createMessage(ChatMessageTypeEnum.FILE,
                "{\"url\":\"https://example.com/a.zip\",\"name\":\"a.zip\",\"size\":0,\"ext\":\"zip\"}");
        ChatMessage nonNumericSize = createMessage(ChatMessageTypeEnum.FILE,
                "{\"url\":\"https://example.com/a.zip\",\"name\":\"a.zip\",\"size\":\"large\",\"ext\":\"zip\"}");

        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(zeroSize));
        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(nonNumericSize));
    }

    @Test
    void shouldNormalizeNonTextContentToPreviewPlaceholder() {
        String normalized = ChatMessageHelper.normalizeStoredContent(ChatMessageTypeEnum.IMAGE.getCode(), null);

        Assertions.assertEquals("[图片]", normalized);
    }

    @Test
    void shouldNormalizeRichMessageContentToPreviewPlaceholders() {
        Assertions.assertEquals("[语音]", ChatMessageHelper.normalizeStoredContent(ChatMessageTypeEnum.VOICE.getCode(), null));
        Assertions.assertEquals("[视频]", ChatMessageHelper.normalizeStoredContent(ChatMessageTypeEnum.VIDEO.getCode(), null));
        Assertions.assertEquals("[表情]", ChatMessageHelper.normalizeStoredContent(ChatMessageTypeEnum.STICKER.getCode(), null));
    }

    private ChatMessage createMessage(ChatMessageTypeEnum type, String extra) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setClientMsgId(type.name().toLowerCase() + "-1");
        chatMessage.setType(type.getCode());
        chatMessage.setExtra(extra);
        return chatMessage;
    }
}
