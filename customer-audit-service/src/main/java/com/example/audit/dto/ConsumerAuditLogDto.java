package com.example.audit.dto;

import java.time.LocalDateTime;

public record ConsumerAuditLogDto (Long id, String eventId, String aggregateType, Long aggregateId, String eventType, String payload, LocalDateTime eventCreatedAt,LocalDateTime consumedAt){
}
