package com.middleware.platform.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;

/**
 * Shared Testcontainers MySQL config. Use via:
 * {@code @Import(TestContainersConfig.class)} on any integration test.
 *
 * <p>Spring Boot auto-detects the {@link ServiceConnection} and overrides
 * the datasource URL/user/password with the container's values.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource")
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>("mysql:8.4")
                .withDatabaseName("yemen_test")
                .withUsername("test")
                .withPassword("test");
    }
}
