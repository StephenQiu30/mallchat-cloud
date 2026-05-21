package com.stephen.cloud.api.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通知批量操作结果响应
 *
 * @author StephenQiu30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通知批量操作结果响应")
public class NotificationBatchOperationResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 请求数量
     */
    @Schema(description = "请求数量")
    private Integer requestedCount;

    /**
     * 实际影响数量
     */
    @Schema(description = "实际影响数量")
    private Integer affectedCount;

    public static NotificationBatchOperationResultVO of(Integer requestedCount, Integer affectedCount) {
        return new NotificationBatchOperationResultVO(requestedCount, affectedCount);
    }
}
