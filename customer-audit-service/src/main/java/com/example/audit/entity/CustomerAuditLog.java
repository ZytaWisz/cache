package com.example.audit.entity;

import com.example.audit.enums.CustomerEventType;
import com.example.audit.listener.AuditListener;
import com.example.audit.model.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_audit_log")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditListener.class)
public class CustomerAuditLog implements Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Enumerated(EnumType.STRING)
    private CustomerEventType customerEventType;

    private Long customerId;

    private String customerName;

    private String customerEmail;

    private LocalDateTime customerCreatedAt;

    private LocalDateTime customerUpdatedAt;

    private LocalDateTime eventConsumedAt;

    @Version
    @Setter(AccessLevel.NONE)
    private Long version;

    public CustomerAuditLog(CustomerEventType customerEventType,
                            Long customerId,
                            String customerName,
                            String customerEmail,
                            LocalDateTime customerCreatedAt,
                            LocalDateTime customerUpdatedAt) {
        this.customerEventType = customerEventType;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerCreatedAt = customerCreatedAt;
        this.customerUpdatedAt = customerUpdatedAt;
    }
}
