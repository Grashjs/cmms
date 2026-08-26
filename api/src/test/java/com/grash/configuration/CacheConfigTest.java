package com.grash.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CacheConfigTest {

    private final CacheConfig cacheConfig = new CacheConfig();

    @Test
    void cacheManager_usersCache_storesAndRetrieves() {
        CacheManager cacheManager = cacheConfig.cacheManager();

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);
        cache.put("key", "value");
        assertEquals("value", cache.get("key").get());
    }

    @Test
    void cacheManager_defaultCache_storesAndRetrieves() {
        CacheManager cacheManager = cacheConfig.cacheManager();

        Cache cache = cacheManager.getCache("someOtherCache");
        assertNotNull(cache);
        cache.put("key", 123);
        assertEquals(123, cache.get("key").get());
    }
}
