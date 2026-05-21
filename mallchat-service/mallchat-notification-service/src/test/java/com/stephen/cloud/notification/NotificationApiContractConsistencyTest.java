package com.stephen.cloud.notification;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.notification.client.NotificationFeignClient;
import com.stephen.cloud.api.notification.model.dto.NotificationCreateRequest;
import com.stephen.cloud.api.notification.model.dto.NotificationIdRequest;
import com.stephen.cloud.api.notification.model.dto.NotificationQueryRequest;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.DeleteRequest;
import com.stephen.cloud.notification.controller.NotificationController;
import com.stephen.cloud.notification.model.entity.Notification;
import com.stephen.cloud.notification.service.impl.NotificationServiceImpl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationApiContractConsistencyTest {

    @Test
    void notificationControllersAndFeignShouldUseDtoRequestsAndVoResponses() {
        List<String> violations = new ArrayList<>();
        int scannedMethodCount = 0;
        for (Class<?> apiType : notificationApiTypes()) {
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
    void notificationCreateRequestRequiredFieldsShouldHaveBeanValidationAnnotations() throws Exception {
        assertHasAnnotation(NotificationCreateRequest.class, "userId", NotNull.class);
        assertHasAnnotation(NotificationCreateRequest.class, "title", NotBlank.class);
        assertHasAnnotation(NotificationCreateRequest.class, "content", NotBlank.class);
        assertHasAnnotation(NotificationCreateRequest.class, "type", NotBlank.class);
    }

    @Test
    void notificationQueryWrapperShouldAllowMissingSortOrder() {
        NotificationQueryRequest request = new NotificationQueryRequest();
        request.setSortField("createTime");

        assertDoesNotThrow(() -> new NotificationServiceImpl().getQueryWrapper(request));
    }

    @Test
    void notificationIdRequestShouldBindFromQueryParameters() {
        NotificationIdRequest request = new NotificationIdRequest();
        DataBinder binder = new DataBinder(request);
        binder.bind(new MutablePropertyValues(Map.of("id", "1")));

        assertEquals(1L, request.getId());
    }

    private static List<Class<?>> notificationApiTypes() {
        return List.of(NotificationController.class, NotificationFeignClient.class);
    }

    private static void assertHasAnnotation(Class<?> type, String fieldName,
                                            Class<? extends java.lang.annotation.Annotation> annotationType)
            throws NoSuchFieldException {
        Field field = type.getDeclaredField(fieldName);
        assertTrue(field.isAnnotationPresent(annotationType),
                type.getSimpleName() + "." + fieldName + " 缺少 " + annotationType.getSimpleName());
    }

    private static void assertRequestDtoContract(Class<?> apiType, Method method, List<String> violations) {
        for (Parameter parameter : method.getParameters()) {
            if (parameter.getAnnotation(RequestParam.class) != null) {
                violations.add(apiType.getSimpleName() + "#" + method.getName() + " 使用 @RequestParam");
            }
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
        if (Boolean.class.equals(dataType) || Long.class.equals(dataType) || Integer.class.equals(dataType)) {
            violations.add(apiType.getSimpleName() + "#" + method.getName() + " 返回 BaseResponse<"
                    + dataType.getTypeName() + ">");
            return;
        }
        if (dataType instanceof ParameterizedType dataParameterizedType
                && List.class.equals(dataParameterizedType.getRawType())
                && Long.class.equals(dataParameterizedType.getActualTypeArguments()[0])) {
            violations.add(apiType.getSimpleName() + "#" + method.getName() + " 返回 BaseResponse<List<Long>>");
            return;
        }
        if (dataType instanceof ParameterizedType dataParameterizedType
                && Page.class.equals(dataParameterizedType.getRawType())
                && Notification.class.equals(dataParameterizedType.getActualTypeArguments()[0])) {
            violations.add(apiType.getSimpleName() + "#" + method.getName() + " 返回 Page<Notification> 实体");
        }
    }
}
