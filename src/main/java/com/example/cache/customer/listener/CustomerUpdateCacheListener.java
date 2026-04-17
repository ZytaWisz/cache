package com.example.cache.customer.listener;

import com.example.cache.customer.event.CustomerEvent;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;
@Component
public class CustomerUpdateCacheListener {

    private final CacheManager cacheManager;

    public CustomerUpdateCacheListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(CustomerEvent event) {
        Objects.requireNonNull(cacheManager.getCache("customerCache")).evict(event.customerId());
    }
}
