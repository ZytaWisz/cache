package com.example.cache;

import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class CompositeCacheTest {
//    @Autowired
//     CustomerRepository customerRepository;
//    @Autowired
//     CustomerService customerService;
//    @Autowired
//    private  CacheManager cacheManager;
//
//    @BeforeEach
//    void cleanDatabase() {
//        customerRepository.deleteAll();
//    }
//
//
//    @Test
//    void givenCustomerIsPresent_whenGetCustomerCalled_thenReturnCustomerAndCacheIt() {
//
//
//        Customer customer = new Customer("test", "test@mail.com");
//        Long customerId=customerRepository.save(customer).getId();
//        // first call → DB
//        CustomerDTO customerCacheMiss = customerService.getCustomer(customerId);
//
//        assertThat(customerCacheMiss.id()).isEqualTo(customer.getId());
//        assertThat(customerCacheMiss.id()).isEqualTo(customer.getId());
//        assertThat(customerCacheMiss.name()).isEqualTo(customer.getName());
//        assertThat(customerCacheMiss.email()).isEqualTo(customer.getEmail());
//
//        // second call → cache
//        customerService.getCustomer(customerId);
//
//        Cache.ValueWrapper wrapper =
//                cacheManager.getCache("customerCache").get(customerId);
//
//        assertThat(wrapper).isNotNull();
//    }
}
