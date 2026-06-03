package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

/**
 * 缓存一致性检查结果 VO
 *
 * @author StephenQiu30
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "缓存一致性检查结果")
public class CacheConsistencyVO implements Serializable {
    @Schema(description = "是否存在漂移")
    private boolean drifted;

    @Schema(description = "DB 中存在但缓存中缺失的用户 ID")
    private Set<Long> missingInCache;

    @Schema(description = "缓存中存在但 DB 中缺失的用户 ID")
    private Set<Long> missingInDb;
}
