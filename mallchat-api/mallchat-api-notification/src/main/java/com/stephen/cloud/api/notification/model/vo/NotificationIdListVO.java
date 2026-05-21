package com.stephen.cloud.api.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 通知 ID 列表响应
 *
 * @author StephenQiu30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通知 ID 列表响应")
public class NotificationIdListVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 通知ID列表
     */
    @Schema(description = "通知ID列表")
    private List<Long> ids;

    public static NotificationIdListVO of(List<Long> ids) {
        return new NotificationIdListVO(ids);
    }
}
