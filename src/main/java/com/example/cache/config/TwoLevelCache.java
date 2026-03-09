package com.example.cache.config;

import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

public class TwoLevelCache implements Cache {

    private final Cache caffeineCache; //L1
    private final Cache redisCache; //L2

    public TwoLevelCache(Cache caffeineCache, Cache redisCache) {
        this.caffeineCache = caffeineCache;
        this.redisCache = redisCache;
    }

    @Override
    public String getName() {
        return caffeineCache.getName();
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public @Nullable ValueWrapper get(Object key) {
        // 1️⃣ L1
        ValueWrapper value = caffeineCache.get(key);
        if (value != null) {
            return value;
        }

        // 2️⃣ L2
        value = redisCache.get(key);
        if (value != null) {
            caffeineCache.put(key, value.get()); // populate L1
            return value;
        }
        return null;
    }

    @Override
    public @Nullable <T> T get(Object key, @Nullable Class<T> type) {
        // 1️⃣ retrieve value using the standard get method
        ValueWrapper wrapper = get(key);

        if (wrapper == null) {
            return null;
        }

        Object value = wrapper.get();

        // 2️⃣ return the value cast to the requested type
        return type != null ? type.cast(value) : (T) value;
    }

    @Override
    public @Nullable <T> T get(Object key, Callable<T> valueLoader) {
        // 1️⃣ Check L1 and L2 using the standard get method
        ValueWrapper wrapper = get(key);

        if (wrapper != null) {
            return (T) wrapper.get();
        }

        try {
            // 2️⃣ Compute the value (e.g., from the database)
            T value = valueLoader.call();

            if (value != null) {
                // 3️⃣ Store the value in both cache levels
                redisCache.put(key, value);
                caffeineCache.put(key, value);
            }

            return value;

        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        redisCache.put(key, value);  // write to Redis (source of truth)
        caffeineCache.put(key, value);  // update local cache

    }

    @Override
    public void evict(Object key) {
        caffeineCache.evict(key);
        redisCache.evict(key);
    }

    @Override
    public void clear() {
        caffeineCache.clear();
        redisCache.clear();

    }
}
