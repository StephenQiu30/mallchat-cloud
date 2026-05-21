package com.stephen.cloud.api.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户 ID 批量请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "用户 ID 批量请求")
public class UserIdsRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID列表
     */
    @NotEmpty(message = "用户ID列表不能为空")
    @Schema(description = "用户ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;

    public static UserIdsRequest of(List<Long> ids) {
        UserIdsRequest request = new UserIdsRequest();
        request.setIds(ids);
        return request;
    }
}
