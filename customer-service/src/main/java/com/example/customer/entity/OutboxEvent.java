package com.example.customer.entity;

import com.example.customer.enums.AggregateType;
import com.example.customer.enums.CustomerEventType;
import com.example.customer.listener.AuditListener;
import com.example.customer.model.Creatable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditListener.class)
public class OutboxEvent implements Creatable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AggregateType aggregateType;

    private Long aggregateId;

    private String eventId;

    @Enumerated(EnumType.STRING)
    private CustomerEventType eventType;

    @Column(columnDefinition = "JSON")
    private String payload;

    private LocalDateTime createdAt;
}
