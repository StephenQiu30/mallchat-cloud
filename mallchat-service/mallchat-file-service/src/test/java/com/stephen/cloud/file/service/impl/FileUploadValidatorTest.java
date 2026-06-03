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

    // --- P0-01: 超大文件拒绝（每种 bizType 对应不同上限） ---

    @Test
    void shouldRejectOversizedUserAvatar() {
        // USER_AVATAR 上限 5MB
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        oversized[0] = (byte) 0x89; oversized[1] = 0x50; oversized[2] = 0x4E; oversized[3] = 0x47;
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", oversized);

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.USER_AVATAR));
    }

    @Test
    void shouldRejectOversizedChatImage() {
        // CHAT_IMAGE 上限 10MB
        byte[] oversized = new byte[10 * 1024 * 1024 + 1];
        oversized[0] = (byte) 0x89; oversized[1] = 0x50; oversized[2] = 0x4E; oversized[3] = 0x47;
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", oversized);

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.CHAT_IMAGE));
    }

    @Test
    void shouldRejectOversizedChatFile() {
        // CHAT_FILE 上限 10MB
        byte[] oversized = new byte[10 * 1024 * 1024 + 1];
        oversized[0] = '%'; oversized[1] = 'P'; oversized[2] = 'D'; oversized[3] = 'F';
        MockMultipartFile file = new MockMultipartFile("file", "big.pdf", "application/pdf", oversized);

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.CHAT_FILE));
    }

    @Test
    void shouldRejectOversizedChatVoice() {
        // CHAT_VOICE 上限 20MB
        byte[] oversized = new byte[20 * 1024 * 1024 + 1];
        oversized[0] = 'I'; oversized[1] = 'D'; oversized[2] = '3';
        MockMultipartFile file = new MockMultipartFile("file", "long.mp3", "audio/mpeg", oversized);

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.CHAT_VOICE));
    }

    @Test
    void shouldRejectOversizedChatVideo() {
        // CHAT_VIDEO 上限 100MB
        byte[] oversized = new byte[100 * 1024 * 1024 + 1];
        oversized[4] = 'f'; oversized[5] = 't'; oversized[6] = 'y'; oversized[7] = 'p';
        MockMultipartFile file = new MockMultipartFile("file", "huge.mp4", "video/mp4", oversized);

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.CHAT_VIDEO));
    }

    // --- P0-01: USER_AVATAR 正向/负向测试 ---

    @Test
    void shouldAcceptValidUserAvatar() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.PNG", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x01});

        FileUploadValidator.ValidatedFile result = validator.validate(file, FileUploadBizEnum.USER_AVATAR);

        Assertions.assertEquals("avatar.PNG", result.fileName());
        Assertions.assertEquals("png", result.suffix());
        Assertions.assertEquals("image/png", result.contentType());
    }

    @Test
    void shouldRejectNonImageUserAvatar() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.exe", "application/octet-stream",
                new byte[]{1, 2, 3});

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.USER_AVATAR));
    }

    // --- P0-01: 危险文件名变体 ---

    @Test
    void shouldRejectBackslashInFileName() {
        MockMultipartFile file = new MockMultipartFile("file", "..\\evil.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.CHAT_IMAGE));
    }

    @Test
    void shouldRejectControlCharInFileName() {
        MockMultipartFile file = new MockMultipartFile("file", "evil .png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.CHAT_IMAGE));
    }

    @Test
    void shouldRejectFileNameExceedingMaxLength() {
        String longName = "a".repeat(129) + ".png";
        MockMultipartFile file = new MockMultipartFile("file", longName, "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.CHAT_IMAGE));
    }

    // --- P0-01: 每种 bizType 伪造 magic byte 负向测试（补充 jpg/gif/webp） ---

    @Test
    void shouldRejectForgedJpgBytes() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg",
                "not-jpg".getBytes());

        Assertions.assertThrows(BusinessException.class, () -> validator.validate(file, FileUploadBizEnum.CHAT_IMAGE));
    }

    @Test
    void shouldAcceptValidJpgImage() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});

        FileUploadValidator.ValidatedFile result = validator.validate(file, FileUploadBizEnum.CHAT_IMAGE);

        Assertions.assertEquals("jpg", result.suffix());
    }

    @Test
    void shouldAcceptValidGifImage() {
        MockMultipartFile file = new MockMultipartFile("file", "anim.gif", "image/gif",
                new byte[]{'G', 'I', 'F', '8', '9', 'a'});

        FileUploadValidator.ValidatedFile result = validator.validate(file, FileUploadBizEnum.CHAT_IMAGE);

        Assertions.assertEquals("gif", result.suffix());
    }

    @Test
    void shouldAcceptValidWebpImage() {
        byte[] webp = new byte[12];
        webp[0] = 'R'; webp[1] = 'I'; webp[2] = 'F'; webp[3] = 'F';
        webp[8] = 'W'; webp[9] = 'E'; webp[10] = 'B'; webp[11] = 'P';
        MockMultipartFile file = new MockMultipartFile("file", "photo.webp", "image/webp", webp);

        FileUploadValidator.ValidatedFile result = validator.validate(file, FileUploadBizEnum.CHAT_IMAGE);

        Assertions.assertEquals("webp", result.suffix());
    }
}
