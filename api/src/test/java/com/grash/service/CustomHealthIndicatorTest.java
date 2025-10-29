package com.grash.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomHealthIndicatorTest {

    @Mock
    private HealthEndpoint healthEndpoint;

    @InjectMocks
    private CustomHealthIndicator customHealthIndicator;

    @Test
    void health_whenUp() {
        HealthComponent healthComponent = Health.up().build();
        when(healthEndpoint.health()).thenReturn(healthComponent);

        Health health = customHealthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
    }

    @Test
    void health_whenDown() {
        HealthComponent healthComponent = Health.down().withDetail("Error", "Health check failed").build();
        when(healthEndpoint.health()).thenReturn(healthComponent);

        Health health = customHealthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("Health check failed", health.getDetails().get("Error"));
    }
}
