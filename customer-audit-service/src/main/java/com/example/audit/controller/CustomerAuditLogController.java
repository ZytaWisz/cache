package com.example.audit.controller;

import com.example.audit.dto.ConsumerAuditLogDto;
import com.example.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerAuditLogController {

    private final AuditLogService service;

    @GetMapping("/customer/logs")
    public Page<ConsumerAuditLogDto> getCustomerLogs(Pageable pageable) {
        return service.getConsumerAuditLogs(pageable);
    }
}
