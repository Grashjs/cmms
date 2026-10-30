package com.grash.controller.analytics;

import com.grash.service.AnalyticsCacheService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * The {@code @Cacheable} keys of the analytics controllers call {@code @analyticsCacheService}. {@code @Import} of
 * the service would register it under its class name, so it is declared here with the name the keys use.
 */
@TestConfiguration
public class AnalyticsCacheKeyConfig {

    @Bean
    public AnalyticsCacheService analyticsCacheService() {
        return new AnalyticsCacheService();
    }
}
