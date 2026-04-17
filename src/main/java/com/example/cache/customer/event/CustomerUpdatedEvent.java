package com.example.cache.customer.event;

public record CustomerUpdatedEvent(Long customerId, String name, String email) {}
