package com.stephen.cloud.chat.support;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.stephen.cloud.api.chat.model.enums.ChatMessageTypeEnum;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * 聊天消息辅助类
 *
 * @author StephenQiu30
 */
public final class ChatMessageHelper {

    private static final Set<String> IMAGE_REQUIRED_FIELDS = Set.of("url", "width", "height", "size");
    private static final Set<String> FILE_REQUIRED_FIELDS = Set.of("url", "name", "size", "ext");

    private ChatMessageHelper() {
    }

    /**
     * 校验消息结构
     *
     * @param chatMessage 聊天消息
     */
    public static void validate(ChatMessage chatMessage) {
        ThrowUtils.throwIf(chatMessage == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StringUtils.isBlank(chatMessage.getClientMsgId()), ErrorCode.PARAMS_ERROR, "客户端消息ID不能为空");

        ChatMessageTypeEnum typeEnum = ChatMessageTypeEnum.getEnumByCode(chatMessage.getType());
        ThrowUtils.throwIf(typeEnum == null, ErrorCode.PARAMS_ERROR, "消息类型不支持");

        switch (typeEnum) {
            case TEXT -> ThrowUtils.throwIf(StringUtils.isBlank(chatMessage.getContent()), ErrorCode.PARAMS_ERROR, "文本消息不能为空");
            case IMAGE -> validateExtra(chatMessage.getExtra(), IMAGE_REQUIRED_FIELDS, "图片消息扩展字段不完整");
            case FILE -> validateExtra(chatMessage.getExtra(), FILE_REQUIRED_FIELDS, "文件消息扩展字段不完整");
            default -> throw new IllegalStateException("Unexpected value: " + typeEnum);
        }
    }

    /**
     * 规范化消息内容，保证 DB 非空约束
     *
     * @param type    消息类型
     * @param content 原始内容
     * @return 规范化后的内容
     */
    public static String normalizeStoredContent(Integer type, String content) {
        if (ChatMessageTypeEnum.TEXT.getCode().equals(type)) {
            return StringUtils.trim(content);
        }
        if (StringUtils.isNotBlank(content)) {
            return StringUtils.trim(content);
        }
        return buildPreview(type, content);
    }

    /**
     * 构造消息预览
     *
     * @param type    消息类型
     * @param content 消息内容
     * @return 预览文案
     */
    public static String buildPreview(Integer type, String content) {
        if (ChatMessageTypeEnum.IMAGE.getCode().equals(type)) {
            return "[图片]";
        }
        if (ChatMessageTypeEnum.FILE.getCode().equals(type)) {
            return "[文件]";
        }
        return StringUtils.defaultString(content);
    }

    private static void validateExtra(String extra, Set<String> requiredFields, String errorMessage) {
        ThrowUtils.throwIf(StringUtils.isBlank(extra), ErrorCode.PARAMS_ERROR, errorMessage);
        JSONObject extraJson;
        try {
            extraJson = JSONUtil.parseObj(extra);
        } catch (Exception e) {
            throw new com.stephen.cloud.common.exception.BusinessException(ErrorCode.PARAMS_ERROR, "消息扩展内容不是合法JSON");
        }
        boolean valid = requiredFields.stream().allMatch(field -> extraJson.containsKey(field) && extraJson.get(field) != null);
        ThrowUtils.throwIf(!valid, ErrorCode.PARAMS_ERROR, errorMessage);
    }
}
