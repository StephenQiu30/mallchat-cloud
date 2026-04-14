package com.stephen.cloud.chat.support;

import com.stephen.cloud.api.chat.model.enums.ChatMessageTypeEnum;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ChatMessageHelperTest {

    @Test
    void shouldAcceptImageMessageWithRequiredExtraFields() {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setClientMsgId("img-1");
        chatMessage.setType(ChatMessageTypeEnum.IMAGE.getCode());
        chatMessage.setExtra("{\"url\":\"https://example.com/a.png\",\"width\":100,\"height\":200,\"size\":4096}");

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
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setClientMsgId("file-1");
        chatMessage.setType(ChatMessageTypeEnum.FILE.getCode());
        chatMessage.setExtra("{\"url\":\"https://example.com/a.zip\",\"name\":\"a.zip\",\"size\":1024}");

        Assertions.assertThrows(BusinessException.class, () -> ChatMessageHelper.validate(chatMessage));
    }

    @Test
    void shouldNormalizeNonTextContentToPreviewPlaceholder() {
        String normalized = ChatMessageHelper.normalizeStoredContent(ChatMessageTypeEnum.IMAGE.getCode(), null);

        Assertions.assertEquals("[图片]", normalized);
    }
}
