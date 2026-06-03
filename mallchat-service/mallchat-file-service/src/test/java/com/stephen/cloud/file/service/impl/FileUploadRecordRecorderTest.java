package com.stephen.cloud.file.service.impl;

import com.stephen.cloud.api.file.model.enums.FileUploadBizEnum;
import com.stephen.cloud.api.file.model.vo.FileVO;
import com.stephen.cloud.api.log.client.LogFeignClient;
import com.stephen.cloud.api.log.model.dto.file.FileUploadRecordAddRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class FileUploadRecordRecorderTest {

    @Test
    void shouldRecordSuccessfulFileUpload() {
        LogFeignClient logFeignClient = Mockito.mock(LogFeignClient.class);
        FileUploadRecordRecorder recorder = new FileUploadRecordRecorder();
        ReflectionTestUtils.setField(recorder, "logFeignClient", logFeignClient);
        FileUploadRecordRecorder.FileUploadMetadata metadata = new FileUploadRecordRecorder.FileUploadMetadata(
                "hello.png", 3L, "png", "image/png");
        FileVO fileVO = FileVO.builder()
                .key("chat_image/hello.png")
                .url("https://example.com/chat_image/hello.png")
                .fileName("hello.png")
                .size(3L)
                .build();

        recorder.recordSuccess(metadata, FileUploadBizEnum.CHAT_IMAGE, fileVO, 1001L, "127.0.0.1");

        ArgumentCaptor<FileUploadRecordAddRequest> captor = ArgumentCaptor.forClass(FileUploadRecordAddRequest.class);
        Mockito.verify(logFeignClient).addFileUploadRecord(captor.capture());
        FileUploadRecordAddRequest request = captor.getValue();
        Assertions.assertEquals(1001L, request.getUserId());
        Assertions.assertEquals("chat_image", request.getBizType());
        Assertions.assertEquals("hello.png", request.getFileName());
        Assertions.assertEquals(3L, request.getFileSize());
        Assertions.assertEquals("png", request.getFileSuffix());
        Assertions.assertEquals("image/png", request.getContentType());
        Assertions.assertEquals("COS", request.getStorageType());
        Assertions.assertEquals("chat_image/hello.png", request.getObjectKey());
        Assertions.assertEquals("https://example.com/chat_image/hello.png", request.getUrl());
        Assertions.assertEquals("127.0.0.1", request.getClientIp());
        Assertions.assertEquals("SUCCESS", request.getStatus());
    }

    @Test
    void shouldRecordFailedFileUpload() {
        LogFeignClient logFeignClient = Mockito.mock(LogFeignClient.class);
        FileUploadRecordRecorder recorder = new FileUploadRecordRecorder();
        ReflectionTestUtils.setField(recorder, "logFeignClient", logFeignClient);
        FileUploadRecordRecorder.FileUploadMetadata metadata = FileUploadRecordRecorder.FileUploadMetadata.from(
                new MockMultipartFile("file", "hello.txt", "text/plain", "abc".getBytes()));

        recorder.recordFailure(metadata, FileUploadBizEnum.CHAT_FILE, 1001L, "127.0.0.1", "文件上传失败");

        ArgumentCaptor<FileUploadRecordAddRequest> captor = ArgumentCaptor.forClass(FileUploadRecordAddRequest.class);
        Mockito.verify(logFeignClient).addFileUploadRecord(captor.capture());
        FileUploadRecordAddRequest request = captor.getValue();
        Assertions.assertEquals("chat_file", request.getBizType());
        Assertions.assertEquals("hello.txt", request.getFileName());
        Assertions.assertEquals("FAIL", request.getStatus());
        Assertions.assertEquals("文件上传失败", request.getErrorMessage());
        Assertions.assertEquals("-", request.getObjectKey());
        Assertions.assertEquals("-", request.getUrl());
    }

    // --- P0-02: 审计记录完整性增强 ---

    @Test
    void shouldRecordAllFiveBizTypes() {
        LogFeignClient logFeignClient = Mockito.mock(LogFeignClient.class);
        FileUploadRecordRecorder recorder = new FileUploadRecordRecorder();
        ReflectionTestUtils.setField(recorder, "logFeignClient", logFeignClient);

        FileUploadBizEnum[] allBizTypes = FileUploadBizEnum.values();
        Assertions.assertEquals(5, allBizTypes.length, "应有 5 种 bizType");

        for (FileUploadBizEnum bizType : allBizTypes) {
            Mockito.reset(logFeignClient);
            FileUploadRecordRecorder.FileUploadMetadata metadata = new FileUploadRecordRecorder.FileUploadMetadata(
                    "test.file", 100L, "file", "application/octet-stream");
            FileVO fileVO = FileVO.builder()
                    .key(bizType.getCode() + "/test.file")
                    .url("https://example.com/" + bizType.getCode() + "/test.file")
                    .fileName("test.file")
                    .size(100L)
                    .build();

            recorder.recordSuccess(metadata, bizType, fileVO, 1001L, "127.0.0.1");

            ArgumentCaptor<FileUploadRecordAddRequest> captor = ArgumentCaptor.forClass(FileUploadRecordAddRequest.class);
            Mockito.verify(logFeignClient).addFileUploadRecord(captor.capture());
            Assertions.assertEquals(bizType.getCode(), captor.getValue().getBizType(),
                    "bizType " + bizType.getCode() + " 应被正确记录");
        }
    }

    @Test
    void shouldSetBucketFromConfiguration() {
        LogFeignClient logFeignClient = Mockito.mock(LogFeignClient.class);
        com.stephen.cloud.file.config.FileStorageConfiguration config =
                Mockito.mock(com.stephen.cloud.file.config.FileStorageConfiguration.class);
        Mockito.when(config.getBucket()).thenReturn("test-bucket");

        FileUploadRecordRecorder recorder = new FileUploadRecordRecorder();
        ReflectionTestUtils.setField(recorder, "logFeignClient", logFeignClient);
        ReflectionTestUtils.setField(recorder, "fileStorageConfiguration", config);

        FileUploadRecordRecorder.FileUploadMetadata metadata = new FileUploadRecordRecorder.FileUploadMetadata(
                "test.png", 100L, "png", "image/png");
        FileVO fileVO = FileVO.builder()
                .key("chat_image/test.png")
                .url("https://example.com/chat_image/test.png")
                .fileName("test.png")
                .size(100L)
                .build();

        recorder.recordSuccess(metadata, FileUploadBizEnum.CHAT_IMAGE, fileVO, 1001L, "127.0.0.1");

        ArgumentCaptor<FileUploadRecordAddRequest> captor = ArgumentCaptor.forClass(FileUploadRecordAddRequest.class);
        Mockito.verify(logFeignClient).addFileUploadRecord(captor.capture());
        Assertions.assertEquals("test-bucket", captor.getValue().getBucket());
    }
}
