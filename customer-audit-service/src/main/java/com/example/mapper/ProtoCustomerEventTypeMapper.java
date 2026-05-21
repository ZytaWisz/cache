package com.example.mapper;

import com.example.audit.enums.CustomerEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class ProtoCustomerEventTypeMapper {

    public CustomerEventType mapCustomerEventType(final com.example.customer.protobuf.enums.CustomerEventType eventType) {
        log.info("CustomerEventType Proto mapping was started");
        return switch (eventType) {
            case CUSTOMER_CREATED -> CustomerEventType.CREATED;
            case CUSTOMER_UPDATED -> CustomerEventType.UPDATED;
            case CUSTOMER_DELETED -> CustomerEventType.DELETED;
            default -> throw  new IllegalArgumentException(String.format("Unknown CustomerEventType %s", eventType));
        };
    }
}
