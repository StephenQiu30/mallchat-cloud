package com.stephen.cloud.common.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用返回类
 *
 * @param <T>
 * @author StephenQiu30
 */
@Data
@Schema(description = "通用返回类")
public class BaseResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 3801016192261040965L;

    /**
     * 状态码
     */
    @Schema(description = "状态码")
    private int code;

    /**
     * 数据
     */
    @Schema(description = "数据")
    private T data;

    /**
     * 消息
     */
    @Schema(description = "消息")
    private String message;

    /**
     * 无参构造，用于 Jackson/Feign 反序列化
     */
    public BaseResponse() {
    }

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
