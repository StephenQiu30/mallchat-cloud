package com.stephen.cloud.mail.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.stephen.cloud.api.mail.model.dto.EmailCodeRequest;
import com.stephen.cloud.api.mail.model.dto.MailSendCodeRequest;
import com.stephen.cloud.api.mail.model.vo.EmailCodeVO;
import com.stephen.cloud.common.cache.model.TimeModel;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.cache.utils.lock.LockUtils;
import com.stephen.cloud.common.cache.utils.ratelimit.RateLimitUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.utils.RegexUtils;
import com.stephen.cloud.mail.properties.EmailCodeProperties;
import com.stephen.cloud.mail.service.EmailCodeService;
import com.stephen.cloud.mail.service.MailService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码业务实现
 * <p>
 * 提供邮箱验证码的申请、生成、频率控制（IP & 邮箱维度）、持久化存储及校验。
 * </p>
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class EmailCodeServiceImpl implements EmailCodeService {

    @Resource
    private EmailCodeProperties emailCodeProperties;

    @Resource
    private RateLimitUtils rateLimitUtils;

    @Resource
    private CacheUtils cacheUtils;

    @Resource
    private MailService mailService;

    @Resource
    private LockUtils lockUtils;

    /**
     * 邮箱验证码缓存 key
     */
    private static final String LOGIN_CODE_EMAIL = "login:code:email:";

    /**
     * 邮箱发送频率限制 key
     */
    private static final String LOGIN_LIMIT_EMAIL = "login:limit:email:";

    /**
     * IP 发送频率限制 key
     */
    private static final String LOGIN_LIMIT_IP = "login:limit:ip:";

    /**
     * 发送邮箱验证码
     * <p>
     * 1. 核心校验：格式校验。
     * 2. 频率控制：防止单 IP 或单邮箱被恶意轰炸。
     * 3. 结果生成：纯数字验证码。
     * 4. 发送逻辑：委托邮件服务进行模板化异步发送。
     * </p>
     *
     * @param request 发送请求包
     * @return 验证码视图封装
     */
    @Override
    public EmailCodeVO sendEmailCode(EmailCodeRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        String email = request.getEmail();
        String clientIp = request.getClientIp();

        ThrowUtils.throwIf(email == null || email.isEmpty(), ErrorCode.PARAMS_ERROR, "收件邮箱不能为空");
        ThrowUtils.throwIf(!RegexUtils.checkEmail(email), ErrorCode.PARAMS_ERROR, "收件邮箱格式非法");
        ThrowUtils.throwIf(clientIp == null || clientIp.isEmpty(), ErrorCode.PARAMS_ERROR, "客户端 IP 不能为空");

        String lockKey = "mail:send:code:" + email;

        // 使用分布式锁，确保同一时间内同一邮箱仅能处理一个验证码申请，防止并发冲突
        return lockUtils.lockEvent(lockKey, () -> {
            // 1. 频率控制：邮箱维度 (每间隔一段时间允许一次)
            String emailLimitKey = LOGIN_LIMIT_EMAIL + email;
            try {
                rateLimitUtils.doRateLimitAndExpire(
                        emailLimitKey,
                        new TimeModel((long) emailCodeProperties.getSendLimit(), TimeUnit.SECONDS),
                        1L,
                        1L,
                        new TimeModel((long) emailCodeProperties.getSendLimit(), TimeUnit.SECONDS));
            } catch (BusinessException e) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "验证码发送过于频繁，请稍后再试");
            }

            // 2. 频率控制：IP 维度 (周期性次数限制)
            String ipLimitKey = LOGIN_LIMIT_IP + clientIp;
            try {
                rateLimitUtils.doRateLimitAndExpire(
                        ipLimitKey,
                        new TimeModel(1L, TimeUnit.HOURS),
                        (long) emailCodeProperties.getIpLimit(),
                        1L,
                        new TimeModel(1L, TimeUnit.HOURS));
            } catch (BusinessException e) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "该 IP 请求验证码次数已达上限");
            }

            // 3. 验证码生成
            String code = RandomUtil.randomNumbers(emailCodeProperties.getLength());
            String codeKey = LOGIN_CODE_EMAIL + email;

            // 4. 委托 MailService 进行模板化发送（内部支持异步）
            try {
                MailSendCodeRequest sendCodeRequest = MailSendCodeRequest.builder()
                        .to(email)
                        .code(code)
                        .minutes(emailCodeProperties.getExpireTime() / 60)
                        .async(true)
                        .build();
                mailService.sendVerificationCode(sendCodeRequest);
                log.info("[EmailCodeServiceImpl] 已触发验证码邮件流程, Email: {}", email);
            } catch (Exception e) {
                log.error("[EmailCodeServiceImpl] 邮件发送流程异常, Email: {}", email, e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮件调度中心异常，请重试");
            }

            // 5. 持久化至缓存
            try {
                cacheUtils.putString(codeKey, code, emailCodeProperties.getExpireTime());
                log.debug("[EmailCodeServiceImpl] 复核码已存入 Redis, Email: {}", email);
            } catch (Exception e) {
                log.error("[EmailCodeServiceImpl] Redis 写入失败, Email: {}", email, e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "核心存储异常，请联系管理员");
            }

            return EmailCodeVO.builder()
                    .expireTime(emailCodeProperties.getExpireTime())
                    .build();
        }, () -> {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "请求处理中，请稍后刷新");
        });
    }

    /**
     * 校验邮箱验证码
     *
     * @param request 校验请求包
     * @return true 若通过
     */
    @Override
    public boolean verifyEmailCode(EmailCodeRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        String email = request.getEmail();
        String code = request.getCode();
        ThrowUtils.throwIf(email == null || email.isEmpty(), ErrorCode.PARAMS_ERROR, "校验邮箱名不能为空");
        ThrowUtils.throwIf(!RegexUtils.checkEmail(email), ErrorCode.PARAMS_ERROR, "校验邮箱名格式非法");
        ThrowUtils.throwIf(code == null || code.isEmpty(), ErrorCode.PARAMS_ERROR, "手动验证码不能为空");

        String codeKey = LOGIN_CODE_EMAIL + email;
        String storedCode = cacheUtils.getString(codeKey);

        if (storedCode == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不存在或已过期");
        }

        if (!storedCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入的验证码有误");
        }

        return true;
    }

    /**
     * 清活验证码 (手动清理)
     *
     * @param email 邮箱地址
     */
    @Override
    public void deleteEmailCode(String email) {
        ThrowUtils.throwIf(email == null || email.isEmpty(), ErrorCode.PARAMS_ERROR, "待清理邮箱不能为空");
        ThrowUtils.throwIf(!RegexUtils.checkEmail(email), ErrorCode.PARAMS_ERROR, "待清理邮箱格式非法");
        String codeKey = LOGIN_CODE_EMAIL + email;
        cacheUtils.remove(codeKey);
    }
}
