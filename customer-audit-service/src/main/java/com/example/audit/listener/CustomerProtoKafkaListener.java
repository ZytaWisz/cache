package com.example.audit.listener;

import com.example.audit.dto.CustomerEventDto;
import com.example.audit.service.AuditLogService;
import com.example.customer.protobuf.event.CustomerEvent;
import com.example.mapper.ProtoCustomerEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerProtoKafkaListener {
    private final AuditLogService auditLogService;
    private final ProtoCustomerEventMapper protoCustomerEventMapper;

    @KafkaListener(
            topics = "customer_proto",
            groupId = "customer-proto-group",
            containerFactory = "protobufKafkaListenerContainerFactory"
    )
    public void listen(CustomerEvent customerEvent) {
        if (customerEvent == null) {
            log.warn("No Received protobuf customer event");
            return;
        }
        CustomerEventDto dto = protoCustomerEventMapper.mapProtoCustomerEventToDTO(customerEvent);

        auditLogService.save(dto);

        log.info(
                "Customer protobuf event saved for customerId={}",
                customerEvent.getCustomerId()
        );
    }
}
