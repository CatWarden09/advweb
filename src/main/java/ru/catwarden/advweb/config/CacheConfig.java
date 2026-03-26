package ru.catwarden.advweb.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.Cache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            @Value("${app.cache.key-prefix:advweb:v2:}") String cacheKeyPrefix
    ) {

        PolymorphicTypeValidator cacheTypeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("ru.catwarden.advweb.")
                .allowIfSubType("org.springframework.data.domain.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.util.")
                .build();

        GenericJacksonJsonRedisSerializer genericSerializer =
                GenericJacksonJsonRedisSerializer.builder()
                        .enableSpringCacheNullValueSupport()
                        .enableDefaultTyping(cacheTypeValidator)
                        .typePropertyName("@class")
                        .build();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues()
                // Versioned prefix avoids breaking reads after serializer changes
                .prefixCacheNameWith(cacheKeyPrefix)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(genericSerializer)
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("categories", config.entryTtl(Duration.ofDays(1)))
                .withCacheConfiguration("advertisements", config.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("users", config.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("advertisements-list", config.entryTtl(Duration.ofMinutes(10)))
                .build();
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache GET failed. Treating as cache-miss. cache={}, key={}", safe(cache), key, exception);
                try {
                    cache.evictIfPresent(key);
                } catch (RuntimeException evictException) {
                    log.debug("Cache evict after GET failure also failed. cache={}, key={}", safe(cache), key, evictException);
                }
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache PUT failed. Ignoring. cache={}, key={}", safe(cache), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache EVICT failed. Ignoring. cache={}, key={}", safe(cache), key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache CLEAR failed. Ignoring. cache={}", safe(cache), exception);
            }

            private String safe(Cache cache) {
                return cache != null ? cache.getName() : "null";
            }
        };
    }
}