package com.example.cache.customer.service;

import com.example.cache.customer.entity.Customer;
import com.example.cache.customer.dto.CustomerDTO;
import com.example.cache.customer.repository.CustomerRepository;
import com.example.cache.event.CustomerEvent;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public CustomerService(CustomerRepository customerRepository, ApplicationEventPublisher applicationEventPublisher) {
        this.customerRepository = customerRepository;
        this.applicationEventPublisher = applicationEventPublisher;
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

        applicationEventPublisher.publishEvent(new CustomerEvent(customer.getId(), customer.getName(),  customer.getEmail()));

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

    @Transactional
    public CustomerDTO updateCustomer(Long id, String name, String email) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setName(name);
        customer.setEmail(email);

        customerRepository.save(customer);
        applicationEventPublisher.publishEvent(new CustomerEvent(customer.getId(), customer.getName(),  customer.getEmail()));

        return new CustomerDTO(customer.getId(), customer.getName(), customer.getEmail());
    }

    public List<CustomerDTO> getCustomers() {
        return customerRepository.findAll().stream()
                .map(customer -> new CustomerDTO(customer.getId(), customer.getName(), customer.getEmail()))
                .toList();
    }
}
