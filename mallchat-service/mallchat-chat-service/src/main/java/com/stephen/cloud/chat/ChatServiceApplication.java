package com.stephen.cloud.chat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 聊天服务启动类
 *
 * @author StephenQiu30
 */
@SpringBootApplication(scanBasePackages = {"com.stephen.cloud.chat", "com.stephen.cloud.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.stephen.cloud.api.*.client")
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@MapperScan("com.stephen.cloud.chat.mapper")
public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}
