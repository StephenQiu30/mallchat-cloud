package com.stephen.cloud.file;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableAsync;

class FileServiceApplicationTest {

    @Test
    void shouldEnableAsyncFileUploadRecordRecorder() {
        Assertions.assertTrue(FileServiceApplication.class.isAnnotationPresent(EnableAsync.class));
    }
}
