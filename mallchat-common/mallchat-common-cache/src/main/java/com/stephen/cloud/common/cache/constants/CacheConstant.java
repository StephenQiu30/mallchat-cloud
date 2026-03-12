package com.stephen.cloud.common.cache.constants;

/**
 * 缓存常量
 *
 * @author StephenQiu30
 */
public interface CacheConstant {

    /**
     * Redis key 文件上传路径前缀
     */
    String FILE_NAME = "stephen:cloud:";

    /**
     * 邮箱验证码 Key 前缀
     */
    String LOGIN_CODE_EMAIL = "login:code:email:";

    /**
     * 邮箱发送频率限制 Key 前缀
     */
    String LOGIN_LIMIT_EMAIL = "login:limit:email:";

    /**
     * IP 发送频率限制 Key 前缀
     */
    String LOGIN_LIMIT_IP = "login:limit:ip:";

    /**
     * GitHub OAuth2 state 前缀
     */
    String GITHUB_OAUTH_STATE = FILE_NAME + "github:oauth:state:";

    /**
     * 微信扫码登录 state 前缀
     */
    String WX_QRCODE_STATE = FILE_NAME + "wx:qrcode:state:";
}
