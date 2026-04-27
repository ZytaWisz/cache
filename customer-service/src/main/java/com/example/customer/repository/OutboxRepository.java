package com.example.customer.repository;

import com.example.customer.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
}
