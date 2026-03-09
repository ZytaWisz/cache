package com.example.cache.kafka;

public record CustomerCreatedEvent(Long id, String name, String email) {
}
