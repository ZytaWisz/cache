package com.example.cache;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
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
       return customerRepository.save(customer).getId();
    }

    public CustomerDTO getCustomerByEmail(String email){
        Optional<Customer> customer=customerRepository.findCustomerByEmail(email);
        if(customer.isPresent()){
            return new CustomerDTO(customer.get().getId(), customer.get().getName(),customer.get().getEmail());
        }else{
            throw new RuntimeException("Customer not found");
        }
    }
}
