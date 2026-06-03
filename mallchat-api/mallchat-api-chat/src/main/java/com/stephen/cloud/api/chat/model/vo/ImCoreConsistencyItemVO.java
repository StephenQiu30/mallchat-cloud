package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 单条 IM 核心表一致性断言结果。
 */
@Data
@Schema(description = "IM 核心表一致性断言结果")
public class ImCoreConsistencyItemVO implements Serializable {

    @Schema(description = "领域：friend/room/message/session/moment")
    private String domain;

    @Schema(description = "断言名称")
    private String name;

    @Schema(description = "孤儿记录数")
    private long orphanCount;

    @Schema(description = "是否通过")
    private boolean passed;
}
