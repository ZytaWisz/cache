package com.example.audit.controller;

import com.example.audit.dto.ConsumerAuditLogDto;
import com.example.audit.service.CustomerAuditLogService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class CustomerAuditLogController {

    private final CustomerAuditLogService service;

    @GetMapping("/customers")
    public List<ConsumerAuditLogDto> getCustomerLogs() {
        return service.getConsumerAuditLogs();
    }
}
