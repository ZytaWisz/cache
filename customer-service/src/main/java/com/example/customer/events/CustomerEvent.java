package com.example.customer.events;

import com.example.customer.enums.CustomerEventType;

import java.time.LocalDateTime;

public record CustomerEvent(CustomerEventType customerEventType, Long customerId, String name, String email, LocalDateTime createdAt,  LocalDateTime updatedAt) {}
