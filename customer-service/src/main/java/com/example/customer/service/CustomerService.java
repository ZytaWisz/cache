package com.example.customer.service;

import com.example.customer.dto.CustomerDTO;
import com.example.customer.entity.Customer;
import com.example.customer.entity.OutboxEvent;
import com.example.customer.enums.AggregateType;
import com.example.customer.enums.CustomerEventType;
import com.example.customer.events.CustomerEvent;
import com.example.customer.exceptions.CustomerNotFoundException;
import com.example.customer.repository.CustomerRepository;
import com.example.customer.repository.OutboxRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Cacheable(cacheNames = "customerCache", key = "#id")
    public CustomerDTO getCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() ->  new CustomerNotFoundException(id));

        return toCustomerDto(customer);
    }

    @Transactional
    public CustomerDTO createCustomer(final String name, final String email){
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

        return toCustomerDto(customer);
    }

    @Transactional
    public CustomerDTO updateCustomer(Long id, String name, String email) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() ->  new CustomerNotFoundException(id));

        customer.setName(name);
        customer.setEmail(email);

        customerRepository.saveAndFlush(customer);


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

        return toCustomerDto(customer);
    }

    public List<CustomerDTO> getCustomers() {
        return customerRepository.findAll().stream()
                .map(this::toCustomerDto)
                .toList();
    }

    private OutboxEvent createOutboxEvent(CustomerEvent customerEvent){
      return new OutboxEvent(
                AggregateType.CUSTOMER,
                customerEvent.customerId(),
                customerEvent.customerEventType(),
                toJson(customerEvent));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    private CustomerDTO toCustomerDto(Customer customer) {
        return new CustomerDTO(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
