package com.example.audit.listener;

import com.example.audit.dto.CustomerEventDto;
import com.example.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;


@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerKafkaListener {

    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    @KafkaListener(
            topics = "customer_topic",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void listen(final String message) {
        try {
            log.info("Receive message from Kafka topic: {}", message);

            CustomerEventDto event = objectMapper.readValue(message, CustomerEventDto.class);

            log.info("Kafka message deserialized to CustomerEventDto: {}", event);

            auditLogService.save(event);

        } catch (JacksonException e) {
            //poison pill- non-retryable
            log.error("Invalid JSON message, skipping: {}", message, e);

        } catch (Exception e) {
            // ✔ transient error → retry
            log.error("Transient error while processing message: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}
