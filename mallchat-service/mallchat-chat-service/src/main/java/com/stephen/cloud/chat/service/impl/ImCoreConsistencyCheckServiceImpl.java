package com.stephen.cloud.chat.service.impl;

import com.stephen.cloud.api.chat.model.vo.ImCoreConsistencyCheckVO;
import com.stephen.cloud.api.chat.model.vo.ImCoreConsistencyItemVO;
import com.stephen.cloud.chat.ops.ImCoreConsistencyAssertion;
import com.stephen.cloud.chat.ops.ImCoreConsistencyAssertions;
import com.stephen.cloud.chat.ops.ImCoreConsistencyQueryPort;
import com.stephen.cloud.chat.service.ImCoreConsistencyCheckService;
import com.stephen.cloud.chat.support.OpsMetricsRecorder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IM 核心表一致性检查实现。
 */
@Slf4j
@Service
public class ImCoreConsistencyCheckServiceImpl implements ImCoreConsistencyCheckService {

    @Resource
    private ImCoreConsistencyQueryPort consistencyQueryPort;

    @Resource
    private OpsMetricsRecorder opsMetricsRecorder;

    @Override
    public ImCoreConsistencyCheckVO checkAll() {
        List<ImCoreConsistencyItemVO> items = new ArrayList<>();
        Map<String, Boolean> domainPassed = new HashMap<>();
        boolean passed = true;

        for (ImCoreConsistencyAssertion assertion : ImCoreConsistencyAssertions.ALL) {
            long orphanCount = consistencyQueryPort.countOrphans(assertion.sql());
            boolean itemPassed = orphanCount == 0L;
            passed &= itemPassed;
            domainPassed.merge(assertion.domain(), itemPassed, (left, right) -> left && right);

            ImCoreConsistencyItemVO item = new ImCoreConsistencyItemVO();
            item.setDomain(assertion.domain());
            item.setName(assertion.name());
            item.setOrphanCount(orphanCount);
            item.setPassed(itemPassed);
            items.add(item);

            if (!itemPassed) {
                log.warn("[ImCoreConsistencyCheck] 断言失败: domain={}, name={}, orphanCount={}",
                        assertion.domain(), assertion.name(), orphanCount);
            }
        }

        domainPassed.forEach((domain, domainOk) ->
                opsMetricsRecorder.recordConsistency(domain, domainOk ? "success" : "failure"));

        ImCoreConsistencyCheckVO result = new ImCoreConsistencyCheckVO();
        result.setPassed(passed);
        result.setItems(items);
        return result;
    }
}
