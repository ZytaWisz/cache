package com.example.cache.customer.controller;

import com.example.cache.customer.dto.CustomerDTO;
import com.example.cache.customer.service.CustomerService;
import com.example.cache.customer.model.CreateCustomerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerController {
    
    private final CustomerService customerService;

    @PostMapping("/customer")
    public Long createCustomer(@RequestBody CreateCustomerRequest createCustomerRequest) {
        return customerService.createCustomer(createCustomerRequest.getName(), createCustomerRequest.getEmail());
    }

    @GetMapping("/customer/{id}")
    public CustomerDTO getCustomer(@PathVariable final String id) {
        return customerService.getCustomer(Long.valueOf(id));
    }

    @GetMapping("/customers")
    public List<CustomerDTO> getCustomers() {
        return customerService.getCustomers();
    }

    @GetMapping("/")
    public String home() {
        return "API works ;)";
    }
}