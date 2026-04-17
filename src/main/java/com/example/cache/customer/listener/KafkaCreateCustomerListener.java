package com.example.cache.customer.listener;

import com.example.cache.customer.event.CustomerCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
@Component
public class KafkaCreateCustomerListener {
    private static final String CUSTOMER_TOPIC = "customer_topic";
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaCreateCustomerListener(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishToKafka(CustomerCreatedEvent event) {
        kafkaTemplate.send(CUSTOMER_TOPIC, String.valueOf(event.customerId()), String.format("CUSTOMER CREATED: ID: %s, name: %s,email: %s.", event.customerId(), event.name(), event.email()));
    }
}
