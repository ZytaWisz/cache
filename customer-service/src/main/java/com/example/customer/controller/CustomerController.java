package com.example.customer.controller;

import com.example.customer.dto.CustomerDTO;
import com.example.customer.service.CustomerService;
import com.example.customer.model.CustomerRequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerController {
    
    private final CustomerService customerService;

    @PostMapping("/customer")
    public Long createCustomer(@RequestBody CustomerRequestBody customerRequestBody) {
        return customerService.createCustomer(customerRequestBody.getName(), customerRequestBody.getEmail());
    }

    @PostMapping("/customer/{id}")
    public CustomerDTO updateCustomer(@PathVariable final String id,@RequestBody CustomerRequestBody customerRequestBody) {
        return customerService.updateCustomer(Long.valueOf(id), customerRequestBody.getName(), customerRequestBody.getEmail());
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