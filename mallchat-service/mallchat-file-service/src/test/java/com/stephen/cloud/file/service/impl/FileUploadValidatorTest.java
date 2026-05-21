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

    @Test
    void shouldAcceptValidChatVoice() {
        MockMultipartFile file = new MockMultipartFile("file", "voice.mp3", "audio/mpeg",
                new byte[]{'I', 'D', '3', 0x04, 0x00});

        FileUploadValidator.ValidatedFile result = validator.validate(file, FileUploadBizEnum.CHAT_VOICE);

        Assertions.assertEquals("mp3", result.suffix());
        Assertions.assertEquals("audio/mpeg", result.contentType());
    }

    @Test
    void shouldRejectForgedChatVoiceBytes() {
        MockMultipartFile file = new MockMultipartFile("file", "voice.mp3", "audio/mpeg",
                "not-audio".getBytes());

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.CHAT_VOICE));
    }

    @Test
    void shouldRejectUnsupportedChatVoiceType() {
        MockMultipartFile file = new MockMultipartFile("file", "voice.exe", "application/octet-stream",
                new byte[]{1, 2, 3, 4});

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.CHAT_VOICE));
    }

    @Test
    void shouldAcceptValidChatVideo() {
        MockMultipartFile file = new MockMultipartFile("file", "video.mp4", "video/mp4",
                new byte[]{0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 'm', 'p', '4', '2'});

        FileUploadValidator.ValidatedFile result = validator.validate(file, FileUploadBizEnum.CHAT_VIDEO);

        Assertions.assertEquals("mp4", result.suffix());
        Assertions.assertEquals("video/mp4", result.contentType());
    }

    @Test
    void shouldRejectForgedChatVideoBytes() {
        MockMultipartFile file = new MockMultipartFile("file", "video.mp4", "video/mp4",
                "not-video".getBytes());

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.CHAT_VIDEO));
    }

    @Test
    void shouldRejectUnsupportedChatVideoType() {
        MockMultipartFile file = new MockMultipartFile("file", "video.exe", "application/octet-stream",
                new byte[]{1, 2, 3, 4});

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.CHAT_VIDEO));
    }
}
