package com.stephen.cloud.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.stephen.cloud.api.log.model.dto.operation.OperationLogQueryRequest;
import com.stephen.cloud.log.model.entity.OperationLog;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Date;

class OperationLogServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OperationLog.class);
    }

    @Test
    void shouldFilterOperationLogByBizId() {
        OperationLogQueryRequest queryRequest = new OperationLogQueryRequest();
        queryRequest.setBizId("room:1");

        LambdaQueryWrapper<OperationLog> wrapper = new OperationLogServiceImpl().getQueryWrapper(queryRequest);

        Assertions.assertTrue(wrapper.getTargetSql().contains("biz_id"), wrapper.getTargetSql());
        Assertions.assertTrue(wrapper.getParamNameValuePairs().containsValue("room:1"));
    }

    @Test
    void shouldFilterOperationLogByOperatorName() {
        OperationLogQueryRequest queryRequest = new OperationLogQueryRequest();
        queryRequest.setOperatorName("Stephen");

        LambdaQueryWrapper<OperationLog> wrapper = new OperationLogServiceImpl().getQueryWrapper(queryRequest);

        Assertions.assertTrue(wrapper.getTargetSql().contains("operator_name"), wrapper.getTargetSql());
        Assertions.assertTrue(wrapper.getParamNameValuePairs().values().stream()
                .map(String::valueOf)
                .anyMatch(value -> value.contains("Stephen")));
    }

    @Test
    void shouldFilterOperationLogByCreateTimeRange() {
        OperationLogQueryRequest queryRequest = new OperationLogQueryRequest();
        Date startTime = new Date(1735660800000L);
        Date endTime = new Date(1735747200000L);
        queryRequest.setStartTime(startTime);
        queryRequest.setEndTime(endTime);

        LambdaQueryWrapper<OperationLog> wrapper = new OperationLogServiceImpl().getQueryWrapper(queryRequest);

        String targetSql = wrapper.getTargetSql();
        Assertions.assertTrue(targetSql.contains("create_time"), targetSql);
        Assertions.assertTrue(targetSql.contains(">="), targetSql);
        Assertions.assertTrue(targetSql.contains("<="), targetSql);
        Assertions.assertTrue(wrapper.getParamNameValuePairs().containsValue(startTime));
        Assertions.assertTrue(wrapper.getParamNameValuePairs().containsValue(endTime));
    }
}
