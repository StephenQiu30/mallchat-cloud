package com.stephen.cloud.chat.service.impl;

import com.stephen.cloud.api.chat.model.dto.RecoveryDryRunRequest;
import com.stephen.cloud.api.chat.model.vo.ImCoreConsistencyCheckVO;
import com.stephen.cloud.api.chat.model.vo.RecoveryDryRunVO;
import com.stephen.cloud.chat.ops.RecoveryDryRunExecutor;
import com.stephen.cloud.chat.support.OpsMetricsRecorder;
import com.stephen.cloud.common.exception.BusinessException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RecoveryDryRunServiceImplTest {

    private RecoveryDryRunServiceImpl recoveryDryRunService;
    private RecordingRecoveryDryRunExecutor executor;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        recoveryDryRunService = new RecoveryDryRunServiceImpl();
        executor = new RecordingRecoveryDryRunExecutor();
        meterRegistry = new SimpleMeterRegistry();
        ReflectionTestUtils.setField(recoveryDryRunService, "recoveryDryRunExecutor", executor);
        ReflectionTestUtils.setField(recoveryDryRunService, "opsMetricsRecorder", new OpsMetricsRecorder(meterRegistry));
    }

    @Test
    void shouldPlanCheckpointsInDryRunModeWithoutTouchingDatabase() {
        RecoveryDryRunRequest request = new RecoveryDryRunRequest();
        request.setDryRun(true);
        request.setRecoveryDatabase("mallchat_recovery_preview");

        RecoveryDryRunVO result = recoveryDryRunService.run(request);

        Assertions.assertTrue(result.isDryRun());
        Assertions.assertTrue(result.isPassed());
        Assertions.assertEquals(5, result.getCheckpoints().size());
        Assertions.assertEquals("planned", result.getCheckpoints().get(1).getStatus());
        Assertions.assertFalse(executor.invoked);
        Assertions.assertEquals(1.0, meterRegistry.get("mallchat.im.recovery.total")
                .tag("phase", "plan")
                .tag("result", "success")
                .counter()
                .count());
    }

    @Test
    void shouldRejectInvalidRecoveryDatabaseName() {
        RecoveryDryRunRequest request = new RecoveryDryRunRequest();
        request.setDryRun(true);
        request.setRecoveryDatabase("bad`name");

        Assertions.assertThrows(BusinessException.class, () -> recoveryDryRunService.run(request));
    }

    @Test
    void shouldDelegateToExecutorWhenDryRunDisabled() {
        RecoveryDryRunRequest request = new RecoveryDryRunRequest();
        request.setDryRun(false);
        request.setRecoveryDatabase("mallchat_recovery_exec");
        request.setBackupFile("src/test/resources/recovery-dry-run-fixture.sql");

        java.nio.file.Path fixture = java.nio.file.Path.of(request.getBackupFile());
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.isRegularFile(fixture),
                "fixture backup file required for execute mode test");

        RecoveryDryRunVO result = recoveryDryRunService.run(request);

        Assertions.assertFalse(result.isDryRun());
        Assertions.assertTrue(result.isPassed());
        Assertions.assertTrue(executor.invoked);
        Assertions.assertNotNull(result.getConsistencyCheck());
    }

    private static class RecordingRecoveryDryRunExecutor implements RecoveryDryRunExecutor {
        private boolean invoked;

        @Override
        public ImCoreConsistencyCheckVO restoreAndVerify(String backupFile, String recoveryDatabase) {
            invoked = true;
            ImCoreConsistencyCheckVO vo = new ImCoreConsistencyCheckVO();
            vo.setPassed(true);
            return vo;
        }
    }
}
