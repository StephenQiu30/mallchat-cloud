package com.stephen.cloud.chat.ops;

import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于 JdbcTemplate 的一致性查询实现。
 */
@Component
public class JdbcImCoreConsistencyQueryPort implements ImCoreConsistencyQueryPort {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public long countOrphans(String sql) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0L : count;
    }
}
