package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.service.CustomHealthIndicator;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health-check")
@RequiredArgsConstructor
public class HealthCheckController {

    private final CustomHealthIndicator healthIndicator;
    @GetMapping("")
    public ResponseEntity<SuccessResponse> healthCheck() {
        Health health = healthIndicator.health();
        if (health.getStatus().equals(Status.DOWN)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new SuccessResponse(false,"Service Unavailable"));
        } else {
            return ResponseEntity.ok().body(new SuccessResponse(true, "Service is up and running"));
        }
    }
}
