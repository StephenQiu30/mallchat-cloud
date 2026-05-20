package com.stephen.cloud.common.web.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class BackendHealthGateConfigTest {

    @Test
    void shouldExposeHealthInfoAndMetricsForDefaultConfig() throws IOException {
        List<PropertySource<?>> sources = loadNacosConfig("common-web.yml");

        Assertions.assertEquals("health,info,metrics", property(sources, "management.endpoints.web.exposure.include"));
        Assertions.assertEquals("true", property(sources, "management.endpoint.health.probes.enabled"));
        Assertions.assertEquals("ping", property(sources, "management.endpoint.health.group.liveness.include"));
        Assertions.assertEquals("ping", property(sources, "management.endpoint.health.group.readiness.include"));
    }

    @Test
    void shouldExposeHealthInfoAndMetricsForProdConfig() throws IOException {
        List<PropertySource<?>> sources = loadNacosConfig("common-web-prod.yml");

        Assertions.assertEquals("health,info,metrics", property(sources, "management.endpoints.web.exposure.include"));
        Assertions.assertEquals("true", property(sources, "management.endpoint.health.probes.enabled"));
        Assertions.assertEquals("when_authorized", property(sources, "management.endpoint.health.show-details"));
        Assertions.assertEquals("ping", property(sources, "management.endpoint.health.group.liveness.include"));
        Assertions.assertEquals("ping", property(sources, "management.endpoint.health.group.readiness.include"));
    }

    @Test
    void shouldAddRedisReadinessForCacheConfig() throws IOException {
        Assertions.assertEquals("ping,redis",
                property(loadNacosConfig("common-cache.yml"), "management.endpoint.health.group.readiness.include"));
        Assertions.assertEquals("ping,redis",
                property(loadNacosConfig("common-cache-prod.yml"), "management.endpoint.health.group.readiness.include"));
    }

    @Test
    void shouldAddDatabaseReadinessForMysqlConfig() throws IOException {
        Assertions.assertEquals("ping,db,redis",
                property(loadNacosConfig("common-mysql.yml"), "management.endpoint.health.group.readiness.include"));
        Assertions.assertEquals("ping,db,redis",
                property(loadNacosConfig("common-mysql-prod.yml"), "management.endpoint.health.group.readiness.include"));
    }

    @Test
    void shouldAddRabbitReadinessForRabbitMqConfig() throws IOException {
        Assertions.assertEquals("ping,db,redis,rabbit",
                property(loadNacosConfig("common-rabbitmq.yml"), "management.endpoint.health.group.readiness.include"));
        Assertions.assertEquals("ping,db,redis,rabbit",
                property(loadNacosConfig("common-rabbitmq-prod.yml"), "management.endpoint.health.group.readiness.include"));
    }

    private List<PropertySource<?>> loadNacosConfig(String fileName) throws IOException {
        Path configPath = findRepositoryRoot().resolve("nacos-config").resolve(fileName);
        return new YamlPropertySourceLoader().load(fileName, new FileSystemResource(configPath));
    }

    private Path findRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("nacos-config/common-web.yml"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位仓库根目录");
    }

    private String property(List<PropertySource<?>> sources, String key) {
        return sources.stream()
                .map(source -> source.getProperty(key))
                .filter(value -> value != null)
                .map(String::valueOf)
                .findFirst()
                .orElse(null);
    }
}
