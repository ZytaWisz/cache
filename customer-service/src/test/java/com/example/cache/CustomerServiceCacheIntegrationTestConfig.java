package com.example.cache;

import com.example.customer.entity.Customer;
import com.example.customer.repository.CustomerRepository;
import com.example.customer.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.test.database.replace=none"
})
public class CustomerServiceCacheIntegrationTestConfig extends RedisIntegrationTestConfig {

    @Autowired
    private CustomerService customerService;

    @MockitoSpyBean
    private CustomerRepository customerRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setup() {
        // ensure clean state
        Objects.requireNonNull(cacheManager.getCache("customerCache")).clear();
        customerRepository.deleteAll();
    }

    @Test
    void shouldUseL1_then_L2() {

        // given
        Customer customer = new Customer("test", "test@mail.com");
        Long id = customerRepository.save(customer).getId();

        // when — first call (DB → L2 → L1)
        customerService.getCustomer(id);

        // second call — should hit L1
        customerService.getCustomer(id);

        // verify cache exists
        assertThat(Objects.requireNonNull(cacheManager.getCache("customerCache"))
                .get(id)).isNotNull();

        // clear L1
        Objects.requireNonNull(cacheManager.getCache("customerCache")).clear();

        // third call — should hit L2, not DB
        customerService.getCustomer(id);

        // still cached
        assertThat(Objects.requireNonNull(cacheManager.getCache("customerCache"))
                .get(id)).isNotNull();
    }

    @Test
    void shouldUseL1ThenL2ThenDb() {

        // given
        Customer customer = new Customer("John", "john@mail.com");
        Long id = customerRepository.save(customer).getId();

        // First call -> DB
        customerService.getCustomer(id);

        verify(customerRepository, times(1)).findById(id);

        // Second call -> L1
        customerService.getCustomer(id);

        // still only one DB call
        verify(customerRepository, times(1)).findById(id);

        // clear L1
        Objects.requireNonNull(cacheManager.getCache("customerCache")).clear();

        // Third call -> L2
        customerService.getCustomer(id);

        // DB still cannot be called again
        verify(customerRepository, times(1)).findById(id);
    }
}

