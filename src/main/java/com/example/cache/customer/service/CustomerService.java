package com.example.cache.customer.service;

import com.example.cache.customer.Customer;
import com.example.cache.customer.CustomerDTO;
import com.example.cache.customer.repository.CustomerRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    private static final String TOPIC = "customer_topic";
    private final KafkaTemplate<String, String> kafkaTemplate;

    public CustomerService(CustomerRepository customerRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.customerRepository = customerRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Cacheable(cacheNames = "customerCache", key = "#id")
    public CustomerDTO getCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return new CustomerDTO(
                customer.getId(),
                customer.getName(),
                customer.getEmail()
        );
    }
@Transactional
    public Long createCustomer(final String name, final String email){
        Customer customer = new Customer();
        customer.setName(name);
        customer.setEmail(email);
        Long customerId = customerRepository.save(customer).getId();
        
        // Send Kafka event
        kafkaTemplate.send(TOPIC, customerId.toString(), String.format("CUSTOMER CREATED: ID: %s, name: %s,email: %s.", customerId, customer.getName(), customer.getEmail()));

        return customerId;
    }

    public CustomerDTO getCustomerByEmail(String email){
        Optional<Customer> customer=customerRepository.findCustomerByEmail(email);
        if(customer.isPresent()){
            return new CustomerDTO(customer.get().getId(), customer.get().getName(),customer.get().getEmail());
        }else{
            throw new RuntimeException("Customer not found");
        }
    }

    @CacheEvict(cacheNames = "customerCache", key = "#id")
    public CustomerDTO updateCustomer(Long id, String name, String email) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setName(name);
        customer.setEmail(email);

        customerRepository.save(customer);

        return new CustomerDTO(customer.getId(), customer.getName(), customer.getEmail());
    }

    public List<CustomerDTO> getCustomers() {
        return customerRepository.findAll().stream()
                .map(customer -> new CustomerDTO(customer.getId(), customer.getName(), customer.getEmail()))
                .toList();
    }
}
