package com.stephen.cloud.api.user.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户管理员状态响应
 *
 * @author StephenQiu30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户管理员状态响应")
public class UserAdminStatusVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否管理员
     */
    @Schema(description = "是否管理员")
    private Boolean admin;

    public static UserAdminStatusVO of(Boolean admin) {
        return new UserAdminStatusVO(admin);
    }
}
