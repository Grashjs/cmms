package com.grash.service;

import com.grash.model.OwnUser;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    public void putUserInCache(OwnUser user) {
        if (user != null) {
            Cache usersCache = cacheManager.getCache(USERS_CACHE);
            if (usersCache != null) {
                Hibernate.initialize(user.getRole());
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

    public Optional<OwnUser> getUserFromCache(String email) {
        Cache usersCache = cacheManager.getCache(USERS_CACHE);
        if (usersCache == null) return Optional.empty();
        OwnUser cachedUser = usersCache.get(getCacheKey(email), OwnUser.class);

        return cachedUser == null ? Optional.empty() : Optional.of(cachedUser);
    }

    private String getCacheKey(String email) {
        return email;
    }
}
