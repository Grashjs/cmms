package com.grash.configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new CaffeineCacheManager() {
            @NotNull
            @Override
            protected Cache<Object, Object> createNativeCaffeineCache(@NotNull String name) {
                return buildSpec(name).build();
            }

            private Caffeine<Object, Object> buildSpec(String name) {
                return switch (name) {
                    case "users" -> Caffeine.newBuilder()
                            .maximumSize(700)
                            .expireAfterWrite(5, TimeUnit.MINUTES);
                    default -> Caffeine.newBuilder()
                            .maximumSize(1000)
                            .expireAfterWrite(20, TimeUnit.MINUTES);
                };
            }
        };
    }

}