package com.stephen.cloud.api.mail.client;

import com.stephen.cloud.api.mail.model.dto.EmailCodeRequest;
import com.stephen.cloud.api.mail.model.dto.MailSendCodeRequest;
import com.stephen.cloud.api.mail.model.dto.MailSendRequest;
import com.stephen.cloud.api.mail.model.vo.EmailCodeVO;
import com.stephen.cloud.common.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 邮件服务 Feign 客户端
 *
 * @author StephenQiu30
 */
@FeignClient(name = "mallchat-mail-service", path = "/api/mail", contextId = "mailFeignClient")
public interface MailFeignClient {

    /**
     * 申请邮箱验证码
     *
     * @param request 邮箱验证码请求
     * @return 验证码视图对象
     */
    @PostMapping("/code/add")
    BaseResponse<EmailCodeVO> addEmailCode(@RequestBody EmailCodeRequest request);

    /**
     * 验证邮箱验证码
     *
     * @param request 邮箱验证码请求（包含邮箱和验证码）
     * @return 是否验证成功
     */
    @PostMapping("/code/verify")
    BaseResponse<Boolean> verifyEmailCode(@RequestBody EmailCodeRequest request);

    /**
     * 删除邮箱验证码
     *
     * @param request 邮箱验证码请求
     * @return 是否删除成功
     */
    @PostMapping("/code/delete")
    BaseResponse<Boolean> deleteEmailCode(@RequestBody EmailCodeRequest request);

    /**
     * 发送邮件（同步）
     *
     * @param request 邮件发送请求
     * @return 是否发送成功
     */
    @PostMapping("/send/sync")
    BaseResponse<Boolean> doSendMailSync(@RequestBody MailSendRequest request);

    /**
     * 发送邮件（异步）
     *
     * @param request 邮件发送请求
     * @return 是否发送成功
     */
    @PostMapping("/send/async")
    BaseResponse<Boolean> doSendMailAsync(@RequestBody MailSendRequest request);

    /**
     * 发送验证码邮件
     *
     * @param request 发送验证码邮件请求
     * @return 是否发送成功
     */
    @PostMapping("/send/verification-code")
    BaseResponse<Boolean> doSendVerificationCode(@RequestBody MailSendCodeRequest request);
}
