package com.stephen.cloud.chat;

import com.stephen.cloud.api.chat.model.dto.ChatFriendQueryRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageReadRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMomentCommentRequest;
import com.stephen.cloud.api.chat.model.dto.ChatPrivateRoomRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChatApiContractConsistencyTest {

    @Test
    void requiredChatRequestFieldsShouldHaveBeanValidationAnnotations() throws Exception {
        assertNotNull(field(ChatPrivateRoomRequest.class, "peerUserId").getAnnotation(NotNull.class));
        assertNotNull(field(ChatMessageReadRequest.class, "roomId").getAnnotation(NotNull.class));
        assertNotNull(field(ChatMessageReadRequest.class, "lastReadMessageId").getAnnotation(NotNull.class));
        assertNotNull(field(ChatMomentCommentRequest.class, "content").getAnnotation(NotBlank.class));
    }

    @Test
    void chatFriendQuerySchemaDescriptionsShouldUseChineseText() throws Exception {
        assertSchemaDescription(ChatFriendQueryRequest.class, "searchText", "关键词（用户昵称）");
        assertSchemaDescription(ChatFriendQueryRequest.class, "userId", "用户ID");
        assertSchemaDescription(ChatFriendQueryRequest.class, "friendUserId", "好友用户ID");
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        return type.getDeclaredField(name);
    }

    private static void assertSchemaDescription(Class<?> type, String fieldName, String expected) throws Exception {
        Schema schema = field(type, fieldName).getAnnotation(Schema.class);
        assertNotNull(schema);
        assertEquals(expected, schema.description());
    }
}
