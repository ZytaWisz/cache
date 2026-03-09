package com.example.cache.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Collection;

@Configuration
@EnableCaching
public class TwoLevelCacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager(
            @Qualifier("caffeineCacheManager") CacheManager caffeineManager,
            @Qualifier("redisCacheManager") CacheManager redisManager) {

        return new CacheManager() {

            @Override
            public Cache getCache(String name) {

                return new TwoLevelCache(
                        caffeineManager.getCache(name),
                        redisManager.getCache(name)
                );
            }

            @Override
            public Collection<String> getCacheNames() {
                return caffeineManager.getCacheNames();
            }
        };
    }
}
