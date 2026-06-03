package com.stephen.cloud.chat.service;

import com.stephen.cloud.api.chat.model.vo.ImCoreConsistencyCheckVO;

/**
 * IM 核心表一致性检查服务。
 */
public interface ImCoreConsistencyCheckService {

    ImCoreConsistencyCheckVO checkAll();
}
