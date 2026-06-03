package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 群治理指标 VO
 *
 * @author StephenQiu30
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "群治理指标")
public class GovernanceMetricsVO implements Serializable {
    @Schema(description = "房间 ID")
    private Long roomId;

    @Schema(description = "总成员数（按角色统计）")
    private int totalMembers;

    @Schema(description = "群主数")
    private int ownerCount;

    @Schema(description = "管理员数")
    private int adminCount;

    @Schema(description = "普通成员数")
    private int memberCount;

    @Schema(description = "最大成员数限制")
    private Integer maxMembers;

    @Schema(description = "当前成员数（DB count）")
    private int currentMemberCount;
}
