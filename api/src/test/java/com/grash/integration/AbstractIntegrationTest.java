package com.grash.integration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@SpringBootTest
public abstract class AbstractIntegrationTest {
    private static volatile PostgreSQLContainer<?> postgresqlContainer;

    // Singleton instance getter
    public static PostgreSQLContainer<?> getPostgresqlContainer() {
        if (postgresqlContainer == null) {
            synchronized (AbstractIntegrationTest.class) {
                if (postgresqlContainer == null) {
                    postgresqlContainer = new PostgreSQLContainer<>(
                            DockerImageName.parse("postgres:16-alpine")
                    )
                            .withDatabaseName("cmms_test")
                            .withUsername("test")
                            .withPassword("test");

                    postgresqlContainer.setWaitStrategy(
                            new LogMessageWaitStrategy()
                                    .withRegEx(".*database system is ready to accept connections.*\\s")
                                    .withTimes(1)
                                    .withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS))
                    );
                }
            }
        }
        return postgresqlContainer;
    }

    @BeforeAll
    protected static void startContainer() {
        PostgreSQLContainer<?> container = getPostgresqlContainer();
        if (!container.isRunning()) {
            container.start();
        }
    }

    @DynamicPropertySource
    public static void overrideProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        PostgreSQLContainer<?> container = getPostgresqlContainer();
        dynamicPropertyRegistry.add("spring.datasource.url", container::getJdbcUrl);
        dynamicPropertyRegistry.add("spring.datasource.username", container::getUsername);
        dynamicPropertyRegistry.add("spring.datasource.password", container::getPassword);
        dynamicPropertyRegistry.add("spring.datasource.driver-class-name", container::getDriverClassName);
        int parallelism = 4;
        int poolSizePerTestClass = 8;
        dynamicPropertyRegistry.add("spring.datasource.hikari.maximum-pool-size",
                () -> String.valueOf(parallelism * poolSizePerTestClass));
        dynamicPropertyRegistry.add("spring.datasource.hikari.minimum-idle", () -> "0");
        dynamicPropertyRegistry.add("spring.datasource.hikari.idle-timeout", () -> "10000");  // 10 seconds
        dynamicPropertyRegistry.add("spring.datasource.hikari.max-lifetime", () -> "120000");   // 2 minutes
        dynamicPropertyRegistry.add("spring.datasource.hikari.leak-detection-threshold", () -> "15000");
    }
}