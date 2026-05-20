package com.stephen.cloud.chat.support;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.stephen.cloud.api.chat.model.enums.ChatMessageTypeEnum;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

/**
 * 聊天消息辅助类
 *
 * @author StephenQiu30
 */
public final class ChatMessageHelper {

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
            case IMAGE -> validateImageExtra(chatMessage.getExtra());
            case FILE -> validateFileExtra(chatMessage.getExtra());
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

    private static void validateImageExtra(String extra) {
        JSONObject extraJson = parseExtra(extra, "图片消息扩展字段不完整");
        boolean valid = hasText(extraJson, "url")
                && hasPositiveNumber(extraJson, "width")
                && hasPositiveNumber(extraJson, "height")
                && hasPositiveNumber(extraJson, "size");
        ThrowUtils.throwIf(!valid, ErrorCode.PARAMS_ERROR, "图片消息扩展字段不完整");
    }

    private static void validateFileExtra(String extra) {
        JSONObject extraJson = parseExtra(extra, "文件消息扩展字段不完整");
        boolean valid = hasText(extraJson, "url")
                && hasText(extraJson, "name")
                && hasText(extraJson, "ext")
                && hasPositiveNumber(extraJson, "size");
        ThrowUtils.throwIf(!valid, ErrorCode.PARAMS_ERROR, "文件消息扩展字段不完整");
    }

    private static JSONObject parseExtra(String extra, String errorMessage) {
        ThrowUtils.throwIf(StringUtils.isBlank(extra), ErrorCode.PARAMS_ERROR, errorMessage);
        try {
            return JSONUtil.parseObj(extra);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息扩展内容不是合法JSON");
        }
    }

    private static boolean hasText(JSONObject extraJson, String field) {
        Object value = extraJson.get(field);
        return value instanceof CharSequence && StringUtils.isNotBlank(value.toString());
    }

    private static boolean hasPositiveNumber(JSONObject extraJson, String field) {
        Object value = extraJson.get(field);
        if (value == null) {
            return false;
        }
        try {
            BigDecimal number = new BigDecimal(value.toString().trim());
            return number.compareTo(BigDecimal.ZERO) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
