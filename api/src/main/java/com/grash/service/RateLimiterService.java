package com.grash.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RateLimiterService {

    private final ConcurrentMap<String, Bucket> demoCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> fileUploadCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> publicMiniCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> authenticatedUserCache = new ConcurrentHashMap<>();

    /**
     * -- GETTER --
     * Check if rate limiting is enabled
     */
    @Getter
    @Value("${security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${security.rate-limit.authenticated.short-term-requests:100}")
    private int authenticatedShortTermRequests;

    @Value("${security.rate-limit.authenticated.short-term-period-minutes:1}")
    private int authenticatedShortTermPeriodMinutes;

    @Value("${security.rate-limit.authenticated.long-term-requests:1000}")
    private int authenticatedLongTermRequests;

    @Value("${security.rate-limit.authenticated.long-term-period-hours:1}")
    private int authenticatedLongTermPeriodHours;

    public Bucket resolveDemoBucket(String key) {
        return demoCache.computeIfAbsent(key, this::newDemoBucket);
    }

    public Bucket resolveFileUploadBucket(String key) {
        return fileUploadCache.computeIfAbsent(key, this::newFileUploadBucket);
    }

    public Bucket resolvePublicMiniBucket(String key) {
        return publicMiniCache.computeIfAbsent(key, this::newPublicMiniBucket);
    }

    private Bucket newDemoBucket(String key) {
        // 1 request per minute
        Bandwidth onePerMinute = Bandwidth.classic(1, Refill.greedy(1, Duration.ofMinutes(1)));

        // 2 requests per 5 hours
        Bandwidth twoPer5Hours = Bandwidth.classic(2, Refill.greedy(2, Duration.ofHours(5)));

        return Bucket.builder()
                .addLimit(onePerMinute)
                .addLimit(twoPer5Hours)
                .build();
    }

    private Bucket newFileUploadBucket(String key) {
        // 1 requests per minute
        Bandwidth tenPerMinute = Bandwidth.classic(4, Refill.greedy(1, Duration.ofMinutes(1)));

        // 4 requests per hour
        Bandwidth fiftyPerHour = Bandwidth.classic(12, Refill.greedy(12, Duration.ofHours(1)));

        return Bucket.builder()
                .addLimit(tenPerMinute)
                .addLimit(fiftyPerHour)
                .build();
    }

    private Bucket newPublicMiniBucket(String key) {
        // 3 requests per minute
        Bandwidth thirtyPerMinute = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));

        //20
        Bandwidth twoHundredPerHour = Bandwidth.classic(60, Refill.greedy(60, Duration.ofHours(1)));

        return Bucket.builder()
                .addLimit(thirtyPerMinute)
                .addLimit(twoHundredPerHour)
                .build();
    }

    /**
     * Resolve rate limit bucket for authenticated users by user ID
     */
    public Bucket resolveAuthenticatedUserBucket(String userId) {
        return authenticatedUserCache.computeIfAbsent(userId, this::newAuthenticatedUserBucket);
    }

    private Bucket newAuthenticatedUserBucket(String key) {
        // Short-term limit: e.g., 100 requests per minute
        Bandwidth shortTerm = Bandwidth.classic(
                authenticatedShortTermRequests,
                Refill.greedy(authenticatedShortTermRequests, Duration.ofMinutes(authenticatedShortTermPeriodMinutes))
        );

        // Long-term limit: e.g., 1000 requests per hour
        Bandwidth longTerm = Bandwidth.classic(
                authenticatedLongTermRequests,
                Refill.greedy(authenticatedLongTermRequests, Duration.ofHours(authenticatedLongTermPeriodHours))
        );

        return Bucket.builder()
                .addLimit(shortTerm)
                .addLimit(longTerm)
                .build();
    }
}
