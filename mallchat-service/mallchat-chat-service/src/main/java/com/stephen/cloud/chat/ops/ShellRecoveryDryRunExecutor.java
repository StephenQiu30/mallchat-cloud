package com.stephen.cloud.chat.ops;

import com.stephen.cloud.api.chat.model.vo.ImCoreConsistencyCheckVO;
import com.stephen.cloud.api.chat.model.vo.ImCoreConsistencyItemVO;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 通过仓库 recovery 脚本在隔离库执行恢复演练。
 */
@Slf4j
@Component
public class ShellRecoveryDryRunExecutor implements RecoveryDryRunExecutor {

    private static final long SCRIPT_TIMEOUT_MINUTES = 30L;

    @Override
    public ImCoreConsistencyCheckVO restoreAndVerify(String backupFile, String recoveryDatabase) {
        Path rootDir = Path.of(System.getProperty("user.dir"));
        Path script = rootDir.resolve("scripts/verify-im-core-data-recovery.sh");
        if (!script.toFile().isFile()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "恢复脚本不存在: " + script);
        }

        ProcessBuilder processBuilder = new ProcessBuilder("bash", script.toString());
        Map<String, String> environment = new HashMap<>(processBuilder.environment());
        environment.put("BACKUP_FILE", backupFile);
        environment.put("RECOVERY_DATABASE", recoveryDatabase);
        environment.put("KEEP_RECOVERY_DB", "false");
        processBuilder.environment().putAll(environment);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(SCRIPT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "恢复脚本执行超时");
            }
            if (process.exitValue() != 0) {
                log.error("[ShellRecoveryDryRunExecutor] 脚本失败, output={}", output);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "恢复脚本执行失败");
            }
            log.info("[ShellRecoveryDryRunExecutor] 脚本成功, recoveryDatabase={}", recoveryDatabase);
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "恢复脚本执行异常: " + ex.getMessage());
        }

        return buildScriptVerifiedResult();
    }

    private static ImCoreConsistencyCheckVO buildScriptVerifiedResult() {
        List<ImCoreConsistencyItemVO> items = new ArrayList<>();
        for (ImCoreConsistencyAssertion assertion : ImCoreConsistencyAssertions.ALL) {
            ImCoreConsistencyItemVO item = new ImCoreConsistencyItemVO();
            item.setDomain(assertion.domain());
            item.setName(assertion.name());
            item.setOrphanCount(0L);
            item.setPassed(true);
            items.add(item);
        }
        ImCoreConsistencyCheckVO result = new ImCoreConsistencyCheckVO();
        result.setPassed(true);
        result.setItems(items);
        return result;
    }
}
