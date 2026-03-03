package com.example.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest
public class CacheTest {
    @Autowired
     CustomerRepository customerRepository;
    @Autowired
     CustomerService customerService;
    @Autowired
    private  CacheManager cacheManager;

    @BeforeEach
    void cleanDatabase() {
        customerRepository.deleteAll();
    }


    @Test
    void givenCustomerIsPresent_whenGetCustomerCalled_thenReturnCustomerAndCacheIt() {


        Customer customer = new Customer("test", "test@mail.com");
        Long customerId=customerRepository.save(customer).getId();
        // first call → DB
        CustomerDTO customerCacheMiss = customerService.getCustomer(customerId);

        assertThat(customerCacheMiss.id()).isEqualTo(customer.getId());
        assertThat(customerCacheMiss.id()).isEqualTo(customer.getId());
        assertThat(customerCacheMiss.name()).isEqualTo(customer.getName());
        assertThat(customerCacheMiss.email()).isEqualTo(customer.getEmail());

        // second call → DB
        customerService.getCustomer(customerId);

        Cache.ValueWrapper wrapper =
                cacheManager.getCache("customerCache").get(customerId);

        assertThat(wrapper).isNotNull();
    }
}
