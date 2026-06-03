package com.stephen.cloud.chat.ops;

/**
 * IM 核心表一致性断言。
 */
public record ImCoreConsistencyAssertion(String domain, String name, String sql) {
}
