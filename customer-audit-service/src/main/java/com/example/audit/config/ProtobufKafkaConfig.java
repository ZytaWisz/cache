package com.example.audit.config;

import com.example.audit.deserializer.ProtoCustomerDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.customer.protobuf.event.CustomerEvent;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.Map;

@Configuration
public class ProtobufKafkaConfig {

    @Bean
    public ConsumerFactory<String, CustomerEvent> protobufCustomerEventConsumerFactory(final KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ProtoCustomerDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "customer-proto-group");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CustomerEvent> protobufKafkaListenerContainerFactory(
            final ConsumerFactory<String, CustomerEvent> protobufCustomerEventConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, CustomerEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(protobufCustomerEventConsumerFactory);

        return factory;
    }
}
