package com.example.mapper;

import com.example.audit.dto.CustomerEventDto;
import com.example.customer.protobuf.event.CustomerEvent;
import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class ProtoCustomerEventMapper {

    private final ProtoCustomerEventTypeMapper typeMapper;
    private final ProtobufTimeMapper timeMapper;

    public CustomerEventDto mapProtoCustomerEventToDTO(CustomerEvent event) {

        return new CustomerEventDto(
                typeMapper.mapCustomerEventType(event.getCustomerEventType()),
                event.getCustomerId(),
                event.getName(),
                event.getEmail(),
                timeMapper.toLocalDateTime(event.getCreatedAt()),
                timeMapper.toLocalDateTime(event.getUpdatedAt())
        );
    }
}
