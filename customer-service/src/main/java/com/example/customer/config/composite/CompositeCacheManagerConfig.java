package com.example.customer.config.composite;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class CompositeCacheManagerConfig {

//    @Bean()
//    @Primary
//    CacheManager cacheManager(@Qualifier("caffeineCacheManager") CacheManager caffeineCacheManager, @Qualifier("redisCacheManager") CacheManager redisCacheManager) {
//        CompositeCacheManager compositeCacheManager = new CompositeCacheManager(caffeineCacheManager, redisCacheManager);
//        compositeCacheManager.setFallbackToNoOpCache(false);
//        return compositeCacheManager;
//    }
}
