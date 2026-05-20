package com.stephen.cloud.api.chat.model.dto;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 动态列表查询请求
 *
 * @author StephenQiu30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "动态列表查询请求")
public class ChatMomentQueryRequest extends PageRequest {

    private static final long serialVersionUID = 1L;
}
