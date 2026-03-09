package com.example.cache;

import com.example.cache.kafka.CustomerCreatedEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    private static final String TOPIC = "customer_topic";
    private final KafkaTemplate<String, CustomerCreatedEvent> kafkaTemplate;

    public CustomerService(CustomerRepository customerRepository, KafkaTemplate<String, CustomerCreatedEvent> kafkaTemplate) {
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

    public Long createCustomer(final String name, final String email){
        Customer customer = new Customer();
        customer.setName(name);
        customer.setEmail(email);
        Long customerId = customerRepository.save(customer).getId();
        
        // Send Kafka event
        if (kafkaTemplate != null) {
            CustomerCreatedEvent event = new CustomerCreatedEvent(customerId, name, email);
            kafkaTemplate.send(TOPIC, customerId.toString(), event);
        }
        
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
}
