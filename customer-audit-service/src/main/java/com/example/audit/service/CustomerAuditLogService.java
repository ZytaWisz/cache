package com.example.audit.service;

import com.example.audit.dto.ConsumerAuditLogDto;
import com.example.audit.dto.CustomerEventDto;
import com.example.audit.entity.CustomerAuditLog;
import com.example.audit.repository.CustomerAuditRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
@Slf4j
@Service
public class CustomerAuditLogService {
    private final ObjectMapper objectMapper;
    private final CustomerAuditRepository repository;

    public CustomerAuditLogService(ObjectMapper objectMapper, CustomerAuditRepository repository) {
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    @Transactional
    @KafkaListener(topics = "customer_topic", groupId = "customer-group")
    public void listen(final String message) {
        try {
            log.info("Receive message from Kafka topic: {}", message);

            CustomerEventDto event = objectMapper.readValue(message, CustomerEventDto.class);

            log.info("Kafka message deserialized to CustomerEventDto: {}", event);

            CustomerAuditLog audit = new CustomerAuditLog(
                    event.customerEventType(),
                    event.customerId(),
                    event.name(),
                    event.email(),
                    event.createdAt(),
                    event.updatedAt());

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
                log.getCustomerId(),
                log.getCustomerEventType(),
                log.getCustomerName(),
                log.getCustomerEmail(),
                log.getCustomerCreatedAt(),
                log.getCustomerUpdatedAt(),
                log.getEventConsumedAt()
        );
    }
}
