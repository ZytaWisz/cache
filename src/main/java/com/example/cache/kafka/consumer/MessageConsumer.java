package com.example.cache.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MessageConsumer {
    @KafkaListener(topics = "customer_topic", groupId = "consumer-cache-group")
    public void listen(final String message) {
        System.out.println("Received message: " + message);
    }
}
