package com.example.audit.dto;
import com.example.audit.enums.CustomerEventType;

import java.time.LocalDateTime;

public record CustomerEventDto(CustomerEventType customerEventType, Long customerId, String name, String email, LocalDateTime createdAt, LocalDateTime updatedAt) {}
