package com.example.audit.repository;

import com.example.audit.entity.CustomerAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerAuditRepository extends JpaRepository<CustomerAuditLog, Long> {
}
