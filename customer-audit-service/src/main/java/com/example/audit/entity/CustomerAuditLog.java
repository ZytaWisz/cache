package com.example.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(unique = true, nullable = false)
    private String eventId;

    private String aggregateType;

    private Long aggregateId;

    private String eventType;

    @Column(columnDefinition = "jsonb")
    private String payload;

    private LocalDateTime eventCreatedAt;

    private LocalDateTime consumedAt;
}
