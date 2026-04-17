package com.example.cache.customer.listener;

import com.example.cache.event.CustomerEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public class KafkaCreateCustomerEvent {
    private static final String CREATE_CUSTOMER_TOPIC = "create_customer";
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaCreateCustomerEvent(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishToKafka(CustomerEvent event) {
        kafkaTemplate.send(CREATE_CUSTOMER_TOPIC, String.valueOf(event.customerId()), String.format("CUSTOMER CREATED: ID: %s, name: %s,email: %s.", event.customerId(), event.name(), event.email()));
    }
}
