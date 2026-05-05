package com.example.customer.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public record CustomerRequestBody(@NotBlank String name, @Email String email) {
}
