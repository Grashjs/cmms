package com.grash.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RateLimiterService {

    private final ConcurrentMap<String, Bucket> demoCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> fileUploadCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> publicMiniCache = new ConcurrentHashMap<>();

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
}
