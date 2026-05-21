package com.stephen.cloud.log;

import com.stephen.cloud.api.log.client.LogFeignClient;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.DeleteRequest;
import com.stephen.cloud.log.controller.ApiAccessLogController;
import com.stephen.cloud.log.controller.FileUploadRecordController;
import com.stephen.cloud.log.controller.OperationLogController;
import com.stephen.cloud.log.controller.UserLoginLogController;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogApiContractConsistencyTest {

    @Test
    void logControllersAndFeignShouldUseDtoRequestsAndVoResponses() {
        List<String> violations = new ArrayList<>();
        int scannedMethodCount = 0;
        for (Class<?> apiType : logApiTypes()) {
            for (Method method : apiType.getDeclaredMethods()) {
                scannedMethodCount++;
                assertRequestDtoContract(apiType, method, violations);
                assertResponseVoContract(apiType, method, violations);
            }
        }
        assertTrue(scannedMethodCount > 0, "契约护栏应至少覆盖 Controller 和 Feign 方法");
        assertEquals(List.of(), violations);
    }

    private static List<Class<?>> logApiTypes() {
        return List.of(
                UserLoginLogController.class,
                OperationLogController.class,
                ApiAccessLogController.class,
                FileUploadRecordController.class,
                LogFeignClient.class
        );
    }

    private static void assertRequestDtoContract(Class<?> apiType, Method method, List<String> violations) {
        for (Parameter parameter : method.getParameters()) {
            if (DeleteRequest.class.equals(parameter.getType())) {
                violations.add(apiType.getSimpleName() + "#" + method.getName() + " 使用通用 DeleteRequest");
            }
        }
    }

    private static void assertResponseVoContract(Class<?> apiType, Method method, List<String> violations) {
        Type returnType = method.getGenericReturnType();
        if (!(returnType instanceof ParameterizedType parameterizedType)
                || !BaseResponse.class.equals(parameterizedType.getRawType())) {
            return;
        }
        Type dataType = parameterizedType.getActualTypeArguments()[0];
        if (Boolean.class.equals(dataType)) {
            violations.add(apiType.getSimpleName() + "#" + method.getName() + " 返回 BaseResponse<Boolean>");
        }
    }
}
