package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 发布动态请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "发布动态请求")
public class ChatMomentPublishRequest implements Serializable {

    @Schema(description = "动态正文", example = "今天状态不错")
    private String content;

    @Schema(description = "动态媒体列表")
    private List<ChatMomentMediaRequest> mediaList;

    @Schema(description = "可见范围：0-好友可见，1-公开", example = "0")
    private Integer visibility;

    private static final long serialVersionUID = 1L;
}
