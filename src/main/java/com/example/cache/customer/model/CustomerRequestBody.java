package com.example.cache.customer.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CustomerRequestBody {
    String name;
    String email;

    public CustomerRequestBody(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
