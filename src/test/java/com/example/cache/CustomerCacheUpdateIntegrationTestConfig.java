package com.example.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class CustomerCacheUpdateIntegrationTestConfig extends RedisIntegrationTestConfig {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void cleanDatabase() {
        customerRepository.deleteAll();
    }

    @Test
    void shouldEvictCacheAfterUpdate() {

        // given
        Customer customer = new Customer("John", "john@test.com");
        Long id = customerRepository.save(customer).getId();

        // first call -> DB -> cache
        CustomerDTO firstCall = customerService.getCustomer(id);

        assertThat(firstCall.name()).isEqualTo("John");

        // verify cache contains value
        Cache.ValueWrapper cached =
                cacheManager.getCache("customerCache").get(id);

        assertThat(cached).isNotNull();
        assertThat(((CustomerDTO) cached.get()).name()).isEqualTo("John");

        // update customer
        customerService.updateCustomer(id, "Mike", "mike@test.com");

        // cache should be evicted
        Cache.ValueWrapper afterEvict =
                cacheManager.getCache("customerCache").get(id);

        assertThat(afterEvict).isNull();

        // next call -> DB again -> cache rebuild
        CustomerDTO secondCall = customerService.getCustomer(id);

        assertThat(secondCall.name()).isEqualTo("Mike");

        // cache should contain updated value
        Cache.ValueWrapper updatedCache =
                cacheManager.getCache("customerCache").get(id);

        assertThat(updatedCache).isNotNull();
        assertThat(((CustomerDTO) updatedCache.get()).name())
                .isEqualTo("Mike");
    }
}
