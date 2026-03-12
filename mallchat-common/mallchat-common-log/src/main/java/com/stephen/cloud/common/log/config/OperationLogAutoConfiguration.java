package com.stephen.cloud.common.log.config;

import com.stephen.cloud.common.log.aspect.OperationLogAspect;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 操作日志自动配置类
 * Spring Boot自动装配会自动加载此配置
 *
 * @author StephenQiu30
 */
@AutoConfiguration
@EnableAspectJAutoProxy
@ComponentScan(basePackageClasses = OperationLogAspect.class)
@Slf4j
public class OperationLogAutoConfiguration {

    /**
     * 依赖注入日志输出
     */
    @PostConstruct
    private void initDi() {
        log.info("############ {} Configuration DI.", this.getClass().getSimpleName().split("\\$\\$")[0]);
    }
}
