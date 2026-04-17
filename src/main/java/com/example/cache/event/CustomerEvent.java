package com.example.cache.event;

public record CustomerEvent(Long customerId, String name, String email) {
}
