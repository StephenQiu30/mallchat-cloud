package com.stephen.cloud.api.log.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 日志操作结果响应
 *
 * @author StephenQiu30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "日志操作结果响应")
public class LogOperationResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    @Schema(description = "是否成功")
    private Boolean success;

    public static LogOperationResultVO of(Boolean success) {
        return new LogOperationResultVO(success);
    }
}
