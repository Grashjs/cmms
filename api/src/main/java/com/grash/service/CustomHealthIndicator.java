package com.grash.service;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@RequiredArgsConstructor
public class CustomHealthIndicator implements HealthIndicator {

    private static final int VALIDATION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        try {
            if (dataSource instanceof HikariDataSource hikariDataSource) {
                HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
                int maxPoolSize = hikariDataSource.getMaximumPoolSize();
                if (pool != null) {
                    int activeConnections = pool.getActiveConnections();
                    int idleConnections = pool.getIdleConnections();
                    int threadsAwaitingConnection = pool.getThreadsAwaitingConnection();
                    builder.withDetail("activeConnections", activeConnections)
                            .withDetail("idleConnections", idleConnections)
                            .withDetail("totalConnections", pool.getTotalConnections())
                            .withDetail("threadsAwaitingConnection", threadsAwaitingConnection)
                            .withDetail("maxPoolSize", maxPoolSize);

                    if (threadsAwaitingConnection > 0 || (activeConnections >= maxPoolSize && idleConnections == 0)) {
                        return builder.down()
                                .withDetail("reason", "Database connection pool exhausted")
                                .build();
                    }
                }
            }

            try (Connection connection = dataSource.getConnection()) {
                if (!connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                    return builder.down()
                            .withDetail("reason", "Database connection is not valid")
                            .build();
                }
            }
            return builder.build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("reason", "Database is unreachable")
                    .build();
        }
    }
}
