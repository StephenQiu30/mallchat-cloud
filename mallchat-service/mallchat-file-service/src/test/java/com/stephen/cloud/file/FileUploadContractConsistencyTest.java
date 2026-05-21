package com.stephen.cloud.file;

import com.stephen.cloud.file.controller.FileController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
