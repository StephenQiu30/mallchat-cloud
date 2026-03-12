package com.stephen.cloud.mail.service;

import com.stephen.cloud.api.mail.model.dto.EmailCodeRequest;
import com.stephen.cloud.api.mail.model.vo.EmailCodeVO;

/**
 * 邮箱验证码服务
 *
 * @author StephenQiu30
 */
public interface EmailCodeService {

    /**
     * 发送邮箱验证码
     *
     * @param request 邮箱验证码请求
     * @return 验证码视图对象
     */
    EmailCodeVO sendEmailCode(EmailCodeRequest request);

    /**
     * 验证邮箱验证码
     *
     * @param request 邮箱验证码请求
     * @return 是否验证成功
     */
    boolean verifyEmailCode(EmailCodeRequest request);

    /**
     * 删除验证码（验证成功后调用）
     *
     * @param email 邮箱地址
     */
    void deleteEmailCode(String email);
}
