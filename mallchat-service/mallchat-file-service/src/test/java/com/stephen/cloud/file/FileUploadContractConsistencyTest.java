package com.stephen.cloud.file;

import com.stephen.cloud.api.file.client.FileFeignClient;
import com.stephen.cloud.api.file.model.enums.FileUploadBizEnum;
import org.springframework.cloud.openfeign.FeignClient;
import com.stephen.cloud.api.file.model.vo.FileVO;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.file.controller.FileController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUploadContractConsistencyTest {

    @Test
    void uploadEndpointShouldKeepMultipartContract() throws NoSuchMethodException {
        Method uploadMethod = FileController.class.getDeclaredMethod("uploadFile", MultipartFile.class, String.class,
                jakarta.servlet.http.HttpServletRequest.class);

        PostMapping postMapping = uploadMethod.getAnnotation(PostMapping.class);

        assertNotNull(postMapping);
        assertArrayEquals(new String[]{MediaType.MULTIPART_FORM_DATA_VALUE}, postMapping.consumes());
    }

    @Test
    void multipartConfigurationShouldAllowLargestBusinessUploadType() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties properties = yaml.getObject();

        assertNotNull(properties);
        assertEquals("100MB", properties.getProperty("spring.servlet.multipart.max-file-size"));
        assertEquals("100MB", properties.getProperty("spring.servlet.multipart.max-request-size"));
    }

    // --- P0-03: bizType 枚举完整性守护 ---

    @Test
    void bizTypeEnumShouldHaveExactlyFiveValues() {
        FileUploadBizEnum[] values = FileUploadBizEnum.values();

        assertEquals(5, values.length, "bizType 枚举应恰好有 5 个值");
    }

    @Test
    void bizTypeEnumShouldCoverAllMediaCategories() {
        String[] codes = java.util.Arrays.stream(FileUploadBizEnum.values())
                .map(FileUploadBizEnum::getCode)
                .toArray(String[]::new);

        assertTrue(java.util.Arrays.asList(codes).contains("user_avatar"), "应包含 user_avatar");
        assertTrue(java.util.Arrays.asList(codes).contains("chat_image"), "应包含 chat_image");
        assertTrue(java.util.Arrays.asList(codes).contains("chat_file"), "应包含 chat_file");
        assertTrue(java.util.Arrays.asList(codes).contains("chat_voice"), "应包含 chat_voice");
        assertTrue(java.util.Arrays.asList(codes).contains("chat_video"), "应包含 chat_video");
    }

    // --- P0-03: Controller 返回类型守护 ---

    @Test
    void uploadEndpointShouldReturnBaseResponseOfFileVO() throws NoSuchMethodException {
        Method uploadMethod = FileController.class.getDeclaredMethod("uploadFile", MultipartFile.class, String.class,
                jakarta.servlet.http.HttpServletRequest.class);

        Type returnType = uploadMethod.getGenericReturnType();
        assertTrue(returnType instanceof ParameterizedType, "返回类型应为参数化类型");
        ParameterizedType parameterizedType = (ParameterizedType) returnType;
        assertEquals(BaseResponse.class, parameterizedType.getRawType(), "返回类型应为 BaseResponse");
        assertEquals(FileVO.class, parameterizedType.getActualTypeArguments()[0], "泛型参数应为 FileVO");
    }

    // --- P0-03: Feign client 路径一致性守护 ---

    @Test
    void feignClientShouldMatchControllerBasePath() {
        RequestMapping controllerMapping = FileController.class.getAnnotation(RequestMapping.class);
        assertNotNull(controllerMapping, "Controller 应有 @RequestMapping 注解");
        String controllerBasePath = controllerMapping.value()[0];

        assertEquals("/file", controllerBasePath, "Controller 基础路径应为 /file");
    }

    @Test
    void feignClientUploadPathShouldMatchControllerEndpoint() throws NoSuchMethodException {
        Method feignMethod = FileFeignClient.class.getDeclaredMethod("uploadFile", MultipartFile.class, String.class);
        PostMapping feignMapping = feignMethod.getAnnotation(PostMapping.class);
        FeignClient feignClient = FileFeignClient.class.getAnnotation(FeignClient.class);

        assertNotNull(feignMapping, "Feign 方法应有 @PostMapping 注解");
        assertNotNull(feignClient, "Feign 接口应有 @FeignClient 注解");
        // Feign client 的 path 包含网关前缀 /api，去掉后应与 Controller 基础路径 + 方法路径一致
        String feignBasePath = feignClient.path();
        String feignMethodPath = feignMapping.value()[0];
        String controllerBasePath = FileController.class.getAnnotation(RequestMapping.class).value()[0];
        Method controllerMethod = FileController.class.getDeclaredMethod("uploadFile", MultipartFile.class, String.class,
                jakarta.servlet.http.HttpServletRequest.class);
        String controllerMethodPath = controllerMethod.getAnnotation(PostMapping.class).value()[0];
        // /api/file/upload 去掉 /api 前缀 == /file/upload
        assertTrue(feignBasePath.endsWith(controllerBasePath),
                "Feign 基础路径应以 Controller 基础路径结尾: " + feignBasePath + " vs " + controllerBasePath);
        assertEquals(controllerMethodPath, feignMethodPath,
                "Feign 方法路径应与 Controller 方法路径一致");
    }
}
