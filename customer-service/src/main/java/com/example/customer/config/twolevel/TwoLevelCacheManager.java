package com.example.customer.config.twolevel;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@RequiredArgsConstructor
public class TwoLevelCacheManager implements CacheManager {

    private final CacheManager caffeineManager;
    private final CacheManager redisManager;

    private final Map<String, Cache> cacheMap = new ConcurrentHashMap<>();

    @Override
    public @Nullable Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, cacheName -> {
            Cache l1 = caffeineManager.getCache(cacheName);
            Cache l2 = redisManager.getCache(cacheName);

            if (l1 == null || l2 == null) {
                throw new IllegalArgumentException("Cache not found: " + cacheName);
            }

            return new TwoLevelCache(l1, l2);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return caffeineManager.getCacheNames();
    }
}
