package com.example.customer.entity;

import com.example.customer.enums.AggregateType;
import com.example.customer.enums.CustomerEventType;
import com.example.customer.listener.AuditListener;
import com.example.customer.model.Creatable;
import com.example.customer.model.EventIdGenerator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "protobuf_outbox_event")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditListener.class)
public class ProtobufOutboxEvent implements Creatable, EventIdGenerator {
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

    @Lob
    @Column(name = "payload")
    private byte[] payload;

    private LocalDateTime createdAt;

    @Version
    @Setter(AccessLevel.NONE)
    private Long version;

    public ProtobufOutboxEvent(AggregateType aggregateType,
                               Long aggregateId,
                               CustomerEventType eventType,
                               byte[] payload) {

        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
    }
}
