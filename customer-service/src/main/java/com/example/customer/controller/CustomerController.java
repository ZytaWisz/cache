package com.example.customer.controller;

import com.example.customer.dto.CustomerDTO;
import com.example.customer.service.CustomerService;
import com.example.customer.model.CustomerRequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerController {
    
    private final CustomerService customerService;

    @PostMapping("/customer")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDTO createCustomer(@RequestBody @Valid CustomerRequestBody customerRequestBody) {
         return customerService.createCustomer(
                 customerRequestBody.name(),
                 customerRequestBody.email());
    }

    @PutMapping("/customer/{id}")
    public CustomerDTO updateCustomer(
            @PathVariable final String id,
            @RequestBody @Valid CustomerRequestBody customerRequestBody) {

        return customerService.updateCustomer(
                Long.valueOf(id),
                customerRequestBody.name(),
                customerRequestBody.email());
    }

    @GetMapping("/customer/{id}")
    public CustomerDTO getCustomer(@PathVariable final Long id) {
        return customerService.getCustomer(id);
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