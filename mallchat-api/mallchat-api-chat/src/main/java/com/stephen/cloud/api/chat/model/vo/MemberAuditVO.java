package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 成员变更审计 VO
 *
 * @author StephenQiu30
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "成员变更审计记录")
public class MemberAuditVO implements Serializable {
    @Schema(description = "审计 ID")
    private Long id;

    @Schema(description = "房间 ID")
    private Long roomId;

    @Schema(description = "被操作用户 ID")
    private Long userId;

    @Schema(description = "操作类型：JOIN/LEAVE/KICK/GRANT_ADMIN/REVOKE_ADMIN")
    private String action;

    @Schema(description = "操作人 ID")
    private Long operatorId;

    @Schema(description = "操作时间")
    private Date createTime;
}
