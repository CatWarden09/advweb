package ru.catwarden.advweb.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setAllowNullValues(false);
        cacheManager.registerCustomCache(
                "categories",
                Caffeine.newBuilder().expireAfterWrite(Duration.ofDays(1)).maximumSize(1_000).build()
        );
        cacheManager.registerCustomCache(
                "favorite-advertisements-list",
                Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(30)).maximumSize(1_000).build()
        );
        cacheManager.registerCustomCache(
                "advertisements-list",
                Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).maximumSize(1_000).build()
        );
        cacheManager.registerCustomCache(
                "comments-list",
                Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(5)).maximumSize(3_000).build()
        );
        cacheManager.registerCustomCache(
                "reviews-approved-list",
                Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).maximumSize(1_000).build()
        );
        cacheManager.registerCustomCache(
                "reviews-approved-user-list",
                Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).maximumSize(3_000).build()
        );
        cacheManager.registerCustomCache(
                "users",
                Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).maximumSize(10_000).build()
        );
        return cacheManager;
    }
}
