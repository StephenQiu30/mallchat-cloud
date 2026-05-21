package com.stephen.cloud.api.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通知操作结果响应
 *
 * @author StephenQiu30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通知操作结果响应")
public class NotificationOperationResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    @Schema(description = "是否成功")
    private Boolean success;

    public static NotificationOperationResultVO of(Boolean success) {
        return new NotificationOperationResultVO(success);
    }
}
