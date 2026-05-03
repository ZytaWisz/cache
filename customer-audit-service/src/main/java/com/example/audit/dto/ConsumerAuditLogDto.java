package com.example.audit.dto;

import com.example.audit.enums.CustomerEventType;

import java.time.LocalDateTime;

public record ConsumerAuditLogDto (Long id, long customerId, CustomerEventType customerEventType, String customerName, String customerEmail, LocalDateTime customerCreatedAt, LocalDateTime customerUpdatedAt, LocalDateTime eventConsumedAt){
}
