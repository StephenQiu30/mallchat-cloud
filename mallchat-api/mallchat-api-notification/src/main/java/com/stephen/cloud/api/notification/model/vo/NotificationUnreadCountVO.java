package com.stephen.cloud.api.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通知未读数响应
 *
 * @author StephenQiu30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通知未读数响应")
public class NotificationUnreadCountVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 未读数量
     */
    @Schema(description = "未读数量")
    private Long count;

    public static NotificationUnreadCountVO of(Long count) {
        return new NotificationUnreadCountVO(count);
    }
}
