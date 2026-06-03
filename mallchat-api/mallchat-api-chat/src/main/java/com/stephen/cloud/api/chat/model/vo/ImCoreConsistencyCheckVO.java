package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * IM 核心表一致性检查结果。
 */
@Data
@Schema(description = "IM 核心表一致性检查结果")
public class ImCoreConsistencyCheckVO implements Serializable {

    @Schema(description = "是否全部通过")
    private boolean passed;

    @Schema(description = "断言明细")
    private List<ImCoreConsistencyItemVO> items;
}
