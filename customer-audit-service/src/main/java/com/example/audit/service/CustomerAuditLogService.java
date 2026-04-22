package com.example.audit.service;

import com.example.audit.dto.ConsumerAuditLogDto;
import com.example.audit.dto.OutboxEventDto;
import com.example.audit.entity.CustomerAuditLog;
import com.example.audit.repository.CustomerAuditRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerAuditLogService {
    private final ObjectMapper objectMapper;
    private final CustomerAuditRepository repository;

    public CustomerAuditLogService(ObjectMapper objectMapper, CustomerAuditRepository repository) {
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    @Transactional
    @KafkaListener(topics = "customer_topic")
    public void listen(final String message) {
        try {
            OutboxEventDto event = objectMapper.readValue(message, OutboxEventDto.class);

            CustomerAuditLog audit = CustomerAuditLog.builder()
                    .eventId(event.eventId())
                    .aggregateType(event.aggregateType())
                    .aggregateId(event.aggregateId())
                    .eventType(event.eventType())
                    .payload(event.payload())
                    .eventCreatedAt(event.createdAt())
                    .consumedAt(LocalDateTime.now())
                    .build();

            repository.save(audit);

        } catch (Exception e) {
            throw new RuntimeException("Failed to process Kafka message", e);
        }
    }

    public List<ConsumerAuditLogDto> getConsumerAuditLogs() {
        return repository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    private ConsumerAuditLogDto toDto(CustomerAuditLog log) {
        return new ConsumerAuditLogDto(
                log.getId(),
                log.getEventId(),
                log.getAggregateType(),
                log.getAggregateId(),
                log.getEventType(),
                log.getPayload(),
                log.getEventCreatedAt(),
                log.getConsumedAt()
        );
    }
}
