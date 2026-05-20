package com.example.customer.repository;

import com.example.customer.entity.ProtobufOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProtobufOutboxRepository extends JpaRepository<ProtobufOutboxEvent, Long> {
}
