package com.example.cache.customer.event;

public record CustomerEvent(Long customerId, String name, String email) {
}
