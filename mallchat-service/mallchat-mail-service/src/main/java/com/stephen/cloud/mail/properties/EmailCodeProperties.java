package com.stephen.cloud.mail.properties;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 登录验证码配置
 *
 * @author StephenQiu30
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "login.email.code")
public class EmailCodeProperties {

    /**
     * 验证码有效期（秒）
     */
    private int expireTime = 300;

    /**
     * 验证码长度
     */
    private int length = 6;

    /**
     * 发送间隔（秒）
     */
    private int sendLimit = 60;

    /**
     * IP 每小时最大发送次数
     */
    private int ipLimit = 10;

    /**
     * 依赖注入日志输出
     */
    @PostConstruct
    private void initDi() {
        log.info("############ {} Configuration DI.", this.getClass().getSimpleName().split("\\$\\$")[0]);
    }
}
