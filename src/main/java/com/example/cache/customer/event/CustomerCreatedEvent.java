package com.example.cache.customer.event;

public record CustomerCreatedEvent(Long customerId, String name, String email) {}
