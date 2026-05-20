package com.stephen.cloud.file.service.impl;

import com.stephen.cloud.api.file.model.enums.FileUploadBizEnum;
import com.stephen.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileUploadValidatorTest {

    private final FileUploadValidator validator = new FileUploadValidator();

    @Test
    void shouldAcceptValidChatImage() {
        MockMultipartFile file = new MockMultipartFile("file", "hello.PNG", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x01});

        FileUploadValidator.ValidatedFile result = validator.validate(file, FileUploadBizEnum.CHAT_IMAGE);

        Assertions.assertEquals("hello.PNG", result.fileName());
        Assertions.assertEquals("png", result.suffix());
        Assertions.assertEquals("image/png", result.contentType());
    }

    @Test
    void shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.CHAT_IMAGE));
    }

    @Test
    void shouldRejectDangerousFileName() {
        MockMultipartFile file = new MockMultipartFile("file", "../evil.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.CHAT_IMAGE));
    }

    @Test
    void shouldRejectForgedImageBytes() {
        MockMultipartFile file = new MockMultipartFile("file", "evil.png", "image/png", "not-image".getBytes());

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.CHAT_IMAGE));
    }

    @Test
    void shouldRejectUnsupportedChatFileType() {
        MockMultipartFile file = new MockMultipartFile("file", "evil.exe", "application/octet-stream",
                new byte[]{1, 2, 3});

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.CHAT_FILE));
    }

    @Test
    void shouldRejectForgedChatFileBytes() {
        MockMultipartFile file = new MockMultipartFile("file", "evil.pdf", "application/pdf",
                "not-pdf".getBytes());

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.CHAT_FILE));
    }

    @Test
    void shouldAcceptAllowedChatFileType() {
        MockMultipartFile file = new MockMultipartFile("file", "readme.pdf", "application/pdf",
                new byte[]{'%', 'P', 'D', 'F'});

        FileUploadValidator.ValidatedFile result = validator.validate(file, FileUploadBizEnum.CHAT_FILE);

        Assertions.assertEquals("pdf", result.suffix());
        Assertions.assertEquals("application/pdf", result.contentType());
    }
}
