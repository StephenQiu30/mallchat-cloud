package com.stephen.cloud.log.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.stephen.cloud.api.log.model.dto.file.FileUploadRecordQueryRequest;
import com.stephen.cloud.log.model.entity.FileUploadRecord;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Date;

class FileUploadRecordServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), FileUploadRecord.class);
    }

    @Test
    void shouldFilterByUserId() {
        FileUploadRecordQueryRequest queryRequest = new FileUploadRecordQueryRequest();
        queryRequest.setUserId(1001L);

        LambdaQueryWrapper<FileUploadRecord> wrapper = new FileUploadRecordServiceImpl().getQueryWrapper(queryRequest);

        Assertions.assertTrue(wrapper.getTargetSql().contains("user_id"), wrapper.getTargetSql());
        Assertions.assertTrue(wrapper.getParamNameValuePairs().containsValue(1001L));
    }

    @Test
    void shouldFilterByBizType() {
        FileUploadRecordQueryRequest queryRequest = new FileUploadRecordQueryRequest();
        queryRequest.setBizType("chat_image");

        LambdaQueryWrapper<FileUploadRecord> wrapper = new FileUploadRecordServiceImpl().getQueryWrapper(queryRequest);

        Assertions.assertTrue(wrapper.getTargetSql().contains("biz_type"), wrapper.getTargetSql());
        Assertions.assertTrue(wrapper.getParamNameValuePairs().containsValue("chat_image"));
    }

    @Test
    void shouldFilterByStatus() {
        FileUploadRecordQueryRequest queryRequest = new FileUploadRecordQueryRequest();
        queryRequest.setStatus("SUCCESS");

        LambdaQueryWrapper<FileUploadRecord> wrapper = new FileUploadRecordServiceImpl().getQueryWrapper(queryRequest);

        Assertions.assertTrue(wrapper.getTargetSql().contains("status"), wrapper.getTargetSql());
        Assertions.assertTrue(wrapper.getParamNameValuePairs().containsValue("SUCCESS"));
    }

    // --- P0-02: 审计日志按时间范围查询 ---

    @Test
    void shouldFilterByCreateTimeRange() {
        FileUploadRecordQueryRequest queryRequest = new FileUploadRecordQueryRequest();
        Date startTime = new Date(1735660800000L);
        Date endTime = new Date(1735747200000L);
        queryRequest.setStartTime(startTime);
        queryRequest.setEndTime(endTime);

        LambdaQueryWrapper<FileUploadRecord> wrapper = new FileUploadRecordServiceImpl().getQueryWrapper(queryRequest);

        String targetSql = wrapper.getTargetSql();
        Assertions.assertTrue(targetSql.contains("create_time"), targetSql);
        Assertions.assertTrue(targetSql.contains(">="), targetSql);
        Assertions.assertTrue(targetSql.contains("<="), targetSql);
        Assertions.assertTrue(wrapper.getParamNameValuePairs().containsValue(startTime));
        Assertions.assertTrue(wrapper.getParamNameValuePairs().containsValue(endTime));
    }

    @Test
    void shouldFilterByUserIdAndTimeRange() {
        FileUploadRecordQueryRequest queryRequest = new FileUploadRecordQueryRequest();
        queryRequest.setUserId(1001L);
        Date startTime = new Date(1735660800000L);
        Date endTime = new Date(1735747200000L);
        queryRequest.setStartTime(startTime);
        queryRequest.setEndTime(endTime);

        LambdaQueryWrapper<FileUploadRecord> wrapper = new FileUploadRecordServiceImpl().getQueryWrapper(queryRequest);

        String targetSql = wrapper.getTargetSql();
        Assertions.assertTrue(targetSql.contains("user_id"), targetSql);
        Assertions.assertTrue(targetSql.contains("create_time"), targetSql);
        Assertions.assertTrue(wrapper.getParamNameValuePairs().containsValue(1001L));
        Assertions.assertTrue(wrapper.getParamNameValuePairs().containsValue(startTime));
        Assertions.assertTrue(wrapper.getParamNameValuePairs().containsValue(endTime));
    }
}
