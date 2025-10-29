package com.grash.service;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private final RateLimiterService rateLimiterService = new RateLimiterService();

    @Test
    void resolveBucket() {
        String key = "test";
        Bucket bucket = rateLimiterService.resolveBucket(key);
        assertNotNull(bucket);

        // First request should be allowed
        assertTrue(bucket.tryConsume(1));

        // Second request should be denied
        assertFalse(bucket.tryConsume(1));
    }
}
