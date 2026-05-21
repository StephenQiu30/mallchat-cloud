package com.stephen.cloud.user;

import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.DeleteRequest;
import com.stephen.cloud.user.controller.UserController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

class UserApiContractConsistencyTest {

    @Test
    void userControllerShouldReturnStableVoForMutationEndpoints() {
        assertResponsePayloadNotRawScalar(UserController.class, "userLogout");
        assertResponsePayloadNotRawScalar(UserController.class, "sendEmailCode");
        assertResponsePayloadNotRawScalar(UserController.class, "addUser");
        assertResponsePayloadNotRawScalar(UserController.class, "deleteUser");
        assertResponsePayloadNotRawScalar(UserController.class, "updateUser");
        assertResponsePayloadNotRawScalar(UserController.class, "editUser");
        assertResponsePayloadNotRawScalar(UserController.class, "isAdmin");
    }

    @Test
    void userControllerShouldNotUseCommonDeleteRequest() {
        for (Method method : UserController.class.getDeclaredMethods()) {
            for (Class<?> parameterType : method.getParameterTypes()) {
                Assertions.assertNotEquals(DeleteRequest.class, parameterType,
                        "用户领域删除接口应定义明确 DTO，避免通用 DeleteRequest 漏校验: " + method.getName());
            }
        }
    }

    @Test
    void userFeignQueryContractShouldUseDtoInsteadOfRequestParam() {
        for (Method method : UserFeignClient.class.getDeclaredMethods()) {
            for (var parameter : method.getParameters()) {
                Assertions.assertNull(parameter.getAnnotation(RequestParam.class),
                        "Feign 查询参数应使用 DTO，便于接口生成和批量参数稳定编码: " + method.getName());
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
