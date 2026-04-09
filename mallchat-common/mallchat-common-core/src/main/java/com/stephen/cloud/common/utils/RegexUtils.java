package com.stephen.cloud.common.utils;

import java.util.regex.Pattern;

/**
 * 正则表达式工具类
 *
 * @author StephenQiu30
 */
public class RegexUtils {


    /**
     * 手机号正则表达式
     * 规则：以 1 开头，第二位为 3-9，后面跟随 9 位数字
     */
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    /**
     * 邮箱正则表达式
     * 规则：简单的邮箱格式校验，包含用户名、@ 符号和域名信息
     */
    public static final String EMAIL_REGEX = "^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$";

    /**
     * 校验邮箱
     *
     * @param email 邮箱地址
     * @return 是否符合正则
     */
    public static boolean checkEmail(String email) {
        return Pattern.matches(EMAIL_REGEX, email);
    }

    /**
     * 校验 URL
     * 规则：包含协议 (http/https/ftp/file) 且符合标准 URL 字符规范
     */
    public static final String URL_REGEX = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$";

    /**
     * 身份证正则表达式 (基础 18 位)
     * 规则：包含 6 位地址码，8 位出生日期，3 位顺序码和 1 位校验码（支持 X）
     */
    public static final String ID_CARD_REGEX = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$";

    /**
     * 数字正则表达式
     * 规则：匹配一个或多个纯数字字符
     */
    public static final String NUMBER_REGEX = "^\\d+$";

    /**
     * 密码正则表达式 (6-20 位，必须包含字母和数字)
     * 规则：通过正向先行断言确保同时包含数字和字母，长度在 6 到 20 之间
     */
    public static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-zA-Z]).{6,20}$";


    /**
     * 校验手机号
     *
     * @param phone 手机号
     * @return 是否符合正则
     */
    public static boolean checkPhone(String phone) {
        return Pattern.matches(PHONE_REGEX, phone);
    }

    /**
     * 校验 URL
     */
    public static boolean checkUrl(String url) {
        return Pattern.matches(URL_REGEX, url);
    }

    /**
     * 校验身份证
     */
    public static boolean checkIdCard(String idCard) {
        return Pattern.matches(ID_CARD_REGEX, idCard);
    }

    /**
     * 校验是否为纯数字
     */
    public static boolean isNumber(String value) {
        return Pattern.matches(NUMBER_REGEX, value);
    }

    /**
     * 校验密码强度
     */
    public static boolean checkPassword(String password) {
        return Pattern.matches(PASSWORD_REGEX, password);
    }
}
