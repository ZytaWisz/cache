package com.example.audit.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
@Slf4j
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler errorHandler() {
        // retry: 3 times every 2 seconds
        FixedBackOff backOff = new FixedBackOff(2000L, 3);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler( backOff);

        errorHandler.setRetryListeners((consumerRecord, ex, deliveryAttempt) ->
            log.warn("Retry attempt {} for record: {}.",
                    deliveryAttempt,
                    consumerRecord.value(),
                    ex)
        );

        return errorHandler;
    }
}
