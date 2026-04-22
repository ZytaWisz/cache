package com.example.audit.dto;

import java.time.LocalDateTime;

public record OutboxEventDto (String eventId, String aggregateType, Long aggregateId, String eventType, String payload, LocalDateTime createdAt){
}
