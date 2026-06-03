package com.stephen.cloud.chat.ops;

/**
 * IM 核心表一致性查询端口（只读 COUNT）。
 */
public interface ImCoreConsistencyQueryPort {

    long countOrphans(String sql);
}
