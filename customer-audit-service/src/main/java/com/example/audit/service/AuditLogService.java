package com.example.audit.service;

import com.example.audit.dto.ConsumerAuditLogDto;
import com.example.audit.dto.CustomerEventDto;
import com.example.audit.entity.CustomerAuditLog;
import com.example.audit.repository.CustomerAuditRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final CustomerAuditRepository repository;

    @Transactional
    public void save(CustomerEventDto event) {

        CustomerAuditLog audit = new CustomerAuditLog(
                event.customerEventType(),
                event.customerId(),
                event.name(),
                event.email(),
                event.createdAt(),
                event.updatedAt()
        );
        repository.save(audit);
    }

    public Page<ConsumerAuditLogDto> getConsumerAuditLogs(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::toDto);
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
