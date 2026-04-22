package com.example.audit.controller;

import com.example.audit.dto.ConsumerAuditLogDto;
import com.example.audit.service.CustomerAuditLogService;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

public class CustomerAuditLogController {

    private final CustomerAuditLogService service;

    public CustomerAuditLogController(CustomerAuditLogService service) {
        this.service = service;
    }

    @GetMapping("/customers")
    public List<ConsumerAuditLogDto> getCustomersLogs() {
        return service.getConsumerAuditLogs();
    }
}
