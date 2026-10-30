package com.grash.service;

import com.grash.dto.DateRange;
import com.grash.model.User;
import com.grash.utils.CacheKeyUtils;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Builds the keys of the analytics caches and invalidates them when the underlying data changes.
 * <p>
 * Every key embeds a per-company "generation" number. Invalidating a company just bumps its generation, so the
 * next request misses the cache and recomputes, while the old entries are never read again and expire on their own
 * (Caffeine size/TTL limits in {@link com.grash.configuration.CacheConfig}).
 * <p>
 * Compared with removing entries, this is O(1) and also safe against the race where a report that started before a
 * commit finishes after it: that stale result is stored under the old generation, which nobody reads anymore.
 * <p>
 * Referenced from the {@code @Cacheable} key expressions as {@code @analyticsCacheService.key(...)}.
 */
@Service
public class AnalyticsCacheService {

    private final AtomicLong globalGeneration = new AtomicLong();
    private final Map<Long, AtomicLong> companyGenerations = new ConcurrentHashMap<>();

    /**
     * Key for analytics that depend on a date range (and, optionally, on its filter fields).
     *
     * @param companyId the company requested by a super account, or {@code null} for the user's own company
     */
    public String key(User user, Long companyId, DateRange dateRange) {
        long effectiveCompanyId = effectiveCompanyId(user, companyId);
        return CacheKeyUtils.analyticsKey(effectiveCompanyId, generation(effectiveCompanyId), user.getId(),
                dateRange);
    }

    /**
     * Key for analytics that do not depend on a date range.
     */
    public String key(User user, Long companyId) {
        return key(user, companyId, null);
    }

    /**
     * Makes every cached report of the company stale.
     */
    public void invalidateCompany(Long companyId) {
        if (companyId == null) {
            invalidateAll();
            return;
        }
        companyGenerations.computeIfAbsent(companyId, id -> new AtomicLong()).incrementAndGet();
    }

    /**
     * Makes every cached report stale, for changes whose company could not be determined.
     */
    public void invalidateAll() {
        globalGeneration.incrementAndGet();
    }

    String generation(long companyId) {
        AtomicLong companyGeneration = companyGenerations.get(companyId);
        return globalGeneration.get() + "." + (companyGeneration == null ? 0 : companyGeneration.get());
    }

    private static long effectiveCompanyId(User user, Long companyId) {
        return companyId != null ? companyId : user.getCompany().getId();
    }
}
