package com.example.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerController {
    
    private final CustomerService customerService;

    @PostMapping("/customer")
    public Long createCustomer(final String name, final String email) {
        return customerService.createCustomer(name, email);
    }

    @GetMapping("/customer/{id}")
    public CustomerDTO getCustomer(@PathVariable final String id) {
        return customerService.getCustomer(Long.valueOf(id));
    }

    @GetMapping("/customers")
    public List<CustomerDTO> getCustomers() {
        return customerService.getCustomers();
    }
}