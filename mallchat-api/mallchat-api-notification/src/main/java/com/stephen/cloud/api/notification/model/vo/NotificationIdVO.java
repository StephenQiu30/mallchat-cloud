package com.stephen.cloud.api.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通知 ID 响应
 *
 * @author StephenQiu30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通知 ID 响应")
public class NotificationIdVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 通知ID
     */
    @Schema(description = "通知ID")
    private Long id;

    public static NotificationIdVO of(Long id) {
        return new NotificationIdVO(id);
    }
}
