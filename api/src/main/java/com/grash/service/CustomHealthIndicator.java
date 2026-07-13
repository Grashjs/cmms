package com.grash.service;

import org.springframework.boot.actuate.health.*;
import org.springframework.stereotype.Component;

@Component
public class CustomHealthIndicator implements HealthIndicator {

    private final HealthEndpoint healthEndpoint;

    public CustomHealthIndicator(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @Override
    public Health health() {
        HealthComponent health = healthEndpoint.health();
        if (health.getStatus().equals(Status.UP)) {
            return Health.up().build();
        } else {
            return Health.down()
                    .withDetail("Error", "Health check failed")
                    .build();
        }
    }
}
