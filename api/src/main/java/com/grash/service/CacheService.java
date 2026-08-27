package com.grash.service;

import com.grash.dto.SignedUrlEntry;
import com.grash.model.File;
import com.grash.model.User;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class CacheService {
    private final CacheManager cacheManager;
    private final String USERS_CACHE = "users";

    public void evictUserFromCache(String email) {
        Cache usersCache = cacheManager.getCache(USERS_CACHE);
        if (usersCache != null) {
            usersCache.evict(getCacheKey(email));
        }
    }

    public void putUserInCache(User user) {
        if (user != null) {
            Cache usersCache = cacheManager.getCache(USERS_CACHE);
            if (usersCache != null) {
                Hibernate.initialize(user.getRole());
                Hibernate.initialize(user.getAppStats());
                if (user.getRole() != null) {
                    Hibernate.initialize(user.getRole().getCreatePermissions());
                    Hibernate.initialize(user.getRole().getViewPermissions());
                    Hibernate.initialize(user.getRole().getViewOtherPermissions());
                    Hibernate.initialize(user.getRole().getDeleteOtherPermissions());
                    Hibernate.initialize(user.getRole().getEditOtherPermissions());
                }
                if (user.getCompany() != null && user.getCompany().getSubscription() != null) {
                    Hibernate.initialize(user.getCompany().getSubscription().getSubscriptionPlan().getFeatures());
                }
                if (user.getEmail() != null)
                    usersCache.put(getCacheKey(user.getEmail()), user);
            }
        }
    }

    public Optional<User> getUserFromCache(String email) {
        Cache usersCache = cacheManager.getCache(USERS_CACHE);
        if (usersCache == null) return Optional.empty();
        User cachedUser = usersCache.get(getCacheKey(email), User.class);

        return cachedUser == null ? Optional.empty() : Optional.of(cachedUser);
    }

    public String getCachedOrGenerateSignedUrl(File file, long expirationMinutes, Supplier<String> urlSupplier) {
        String key = file.getPath() + ":" + expirationMinutes;
        Cache cache = cacheManager.getCache("signedUrls");

        if (cache != null) {
            SignedUrlEntry cached = cache.get(key, SignedUrlEntry.class);
            if (cached != null && !cached.isExpired()) {
                return cached.url();
            }
        }

        String freshUrl = urlSupplier.get();

        if (cache != null) {
            Instant expiresAt = Instant.now()
                    .plusSeconds(expirationMinutes * 60)
                    .minusSeconds(30);
            cache.put(key, new SignedUrlEntry(freshUrl, expiresAt));
        }

        return freshUrl;
    }

    private String getCacheKey(String email) {
        return email;
    }
}
