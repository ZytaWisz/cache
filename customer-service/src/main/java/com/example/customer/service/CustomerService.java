package com.example.customer.service;

import com.example.customer.dto.CustomerDTO;
import com.example.customer.entity.Customer;
import com.example.customer.entity.OutboxEvent;
import com.example.customer.enums.AggregateType;
import com.example.customer.enums.CustomerEventType;
import com.example.customer.events.CustomerEvent;
import com.example.customer.repository.CustomerRepository;
import com.example.customer.repository.OutboxRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OutboxRepository outboxRepository;

    public CustomerService(CustomerRepository customerRepository, ApplicationEventPublisher applicationEventPublisher, OutboxRepository outboxRepository) {
        this.customerRepository = customerRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.outboxRepository = outboxRepository;
    }

    @Cacheable(cacheNames = "customerCache", key = "#id")
    public CustomerDTO getCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return new CustomerDTO(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    @Transactional
    public Long createCustomer(final String name, final String email){
        Customer customer = new Customer();
        customer.setName(name);
        customer.setEmail(email);

         customerRepository.save(customer);

        CustomerEvent event=new CustomerEvent(
                CustomerEventType.CREATED,
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());


        outboxRepository.save(createOutboxEvent(event));

        return customer.getId();
    }

    public CustomerDTO getCustomerByEmail(String email){
        Optional<Customer> customer=customerRepository.findCustomerByEmail(email);
        if(customer.isPresent()){
            return new CustomerDTO(
                    customer.get().getId(),
                    customer.get().getName(),
                    customer.get().getEmail(),
                    customer.get().getCreatedAt(),
                    customer.get().getUpdatedAt());
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

        CustomerEvent event=new CustomerEvent(
                CustomerEventType.UPDATED,
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());

        // sending event to CustomerCacheListener
        applicationEventPublisher.publishEvent(event);

        outboxRepository.save(createOutboxEvent(event));

        return new CustomerDTO(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }

    public List<CustomerDTO> getCustomers() {
        return customerRepository.findAll().stream()
                .map(customer -> new CustomerDTO(
                        customer.getId(),
                        customer.getName(),
                        customer.getEmail(),
                        customer.getCreatedAt(),
                        customer.getUpdatedAt()))
                .toList();
    }

    private OutboxEvent createOutboxEvent(CustomerEvent customerEvent){
      return  OutboxEvent.builder()
                .aggregateType(AggregateType.CUSTOMER)
                .aggregateId(customerEvent.customerId())
                .eventType(customerEvent.customerEventType())
                .payload(toJson(customerEvent))
                .build();
    }

    private String toJson(Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
