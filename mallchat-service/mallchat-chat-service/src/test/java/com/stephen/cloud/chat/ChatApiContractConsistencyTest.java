package com.stephen.cloud.chat;

import com.stephen.cloud.api.chat.client.ChatFeignClient;
import com.stephen.cloud.api.chat.model.dto.ChatFriendQueryRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageReadRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageSearchRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMomentCommentRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMomentIdRequest;
import com.stephen.cloud.api.chat.model.dto.ChatPrivateRoomRequest;
import com.stephen.cloud.chat.controller.ChatFriendApplyController;
import com.stephen.cloud.chat.controller.ChatFriendController;
import com.stephen.cloud.chat.controller.ChatMessageController;
import com.stephen.cloud.chat.controller.ChatMomentController;
import com.stephen.cloud.chat.controller.ChatReportController;
import com.stephen.cloud.chat.controller.ChatRoomController;
import com.stephen.cloud.chat.controller.ChatRoomJoinApplyController;
import com.stephen.cloud.chat.controller.ChatSessionController;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.DeleteRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void chatControllersShouldUseRequestDtoAndResponseVoContracts() {
        List<String> violations = new ArrayList<>();
        int scannedMethodCount = 0;
        for (Class<?> apiType : chatApiTypes()) {
            for (Method method : apiType.getDeclaredMethods()) {
                scannedMethodCount++;
                assertRequestDtoContract(apiType, method, violations);
                assertResponseVoContract(apiType, method, violations);
            }
        }
        assertTrue(scannedMethodCount > 0, "契约护栏应至少覆盖 Controller 和 Feign 方法");
        assertEquals(List.of(), violations);
    }

    @Test
    void legacyQueryContractDefaultsShouldStayStable() throws Exception {
        assertNotNull(field(ChatMomentIdRequest.class, "id"));
        assertEquals(20L, new ChatMessageSearchRequest().getPageSize());
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        return type.getDeclaredField(name);
    }

    private static void assertSchemaDescription(Class<?> type, String fieldName, String expected) throws Exception {
        Schema schema = field(type, fieldName).getAnnotation(Schema.class);
        assertNotNull(schema);
        assertEquals(expected, schema.description());
    }

    private static List<Class<?>> chatApiTypes() {
        return List.of(
                ChatFriendApplyController.class,
                ChatFriendController.class,
                ChatMessageController.class,
                ChatMomentController.class,
                ChatReportController.class,
                ChatRoomController.class,
                ChatRoomJoinApplyController.class,
                ChatSessionController.class,
                ChatFeignClient.class
        );
    }

    private static void assertRequestDtoContract(Class<?> controller, Method method, List<String> violations) {
        for (Parameter parameter : method.getParameters()) {
            if (parameter.getAnnotation(RequestParam.class) != null) {
                violations.add(controller.getSimpleName() + "#" + method.getName() + " 使用 @RequestParam");
            }
            if (DeleteRequest.class.equals(parameter.getType())) {
                violations.add(controller.getSimpleName() + "#" + method.getName() + " 使用通用 DeleteRequest");
            }
        }
    }

    private static void assertResponseVoContract(Class<?> controller, Method method, List<String> violations) {
        Type returnType = method.getGenericReturnType();
        if (!(returnType instanceof ParameterizedType parameterizedType)
                || !BaseResponse.class.equals(parameterizedType.getRawType())) {
            return;
        }
        Type dataType = parameterizedType.getActualTypeArguments()[0];
        if (Boolean.class.equals(dataType) || Long.class.equals(dataType)) {
            violations.add(controller.getSimpleName() + "#" + method.getName() + " 返回 BaseResponse<" + dataType.getTypeName() + ">");
        }
    }
}
