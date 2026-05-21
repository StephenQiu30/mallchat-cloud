package com.stephen.cloud.ai;

import com.stephen.cloud.ai.controller.AiChatRecordController;
import com.stephen.cloud.api.ai.client.AiFeignClient;
import com.stephen.cloud.api.ai.model.dto.AiChatRequest;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.DeleteRequest;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

class AiApiContractConsistencyTest {

    @Test
    void aiMutationEndpointsShouldReturnStableVo() {
        assertResponsePayloadNotRawScalar(AiChatRecordController.class, "deleteAiChatRecord");
        assertResponsePayloadNotRawScalar(AiFeignClient.class, "deleteAiChatRecord");
    }

    @Test
    void aiDeleteEndpointShouldUseDomainDto() {
        assertNoCommonDeleteRequest(AiChatRecordController.class);
        assertNoCommonDeleteRequest(AiFeignClient.class);
    }

    @Test
    void aiChatMessageShouldDeclareRequiredValidation() throws NoSuchFieldException {
        var messageField = AiChatRequest.class.getDeclaredField("message");
        Assertions.assertNotNull(messageField.getAnnotation(NotBlank.class),
                "AI 对话消息必须声明 @NotBlank，Controller 再配合 @Validated 触发统一校验");
    }

    private void assertNoCommonDeleteRequest(Class<?> targetClass) {
        for (Method method : targetClass.getDeclaredMethods()) {
            for (Class<?> parameterType : method.getParameterTypes()) {
                Assertions.assertNotEquals(DeleteRequest.class, parameterType,
                        "AI 删除接口应定义明确 DTO，避免通用 DeleteRequest 漏校验: " + method.getName());
            }
        }
    }

    private void assertResponsePayloadNotRawScalar(Class<?> targetClass, String methodName) {
        Method method = findMethod(targetClass, methodName);
        Assertions.assertEquals(BaseResponse.class, method.getReturnType());
        Type genericReturnType = method.getGenericReturnType();
        Assertions.assertInstanceOf(ParameterizedType.class, genericReturnType);
        Type payloadType = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
        Assertions.assertNotEquals(Boolean.class, payloadType,
                methodName + " 不应返回 BaseResponse<Boolean>，应返回领域 VO");
        Assertions.assertNotEquals(Long.class, payloadType,
                methodName + " 不应返回 BaseResponse<Long>，应返回领域 VO");
    }

    private Method findMethod(Class<?> targetClass, String methodName) {
        for (Method method : targetClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new AssertionError("缺少方法: " + methodName);
    }
}
