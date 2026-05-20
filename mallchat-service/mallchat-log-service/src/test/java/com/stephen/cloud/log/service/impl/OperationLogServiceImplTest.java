package com.stephen.cloud.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.stephen.cloud.api.log.model.dto.operation.OperationLogQueryRequest;
import com.stephen.cloud.log.model.entity.OperationLog;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OperationLogServiceImplTest {

    @Test
    void shouldFilterOperationLogByBizId() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OperationLog.class);
        OperationLogQueryRequest queryRequest = new OperationLogQueryRequest();
        queryRequest.setBizId("room:1");

        LambdaQueryWrapper<OperationLog> wrapper = new OperationLogServiceImpl().getQueryWrapper(queryRequest);

        Assertions.assertTrue(wrapper.getTargetSql().contains("biz_id"), wrapper.getTargetSql());
    }
}
