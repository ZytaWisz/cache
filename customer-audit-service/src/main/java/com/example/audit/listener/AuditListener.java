package com.example.audit.listener;

import com.example.audit.model.Auditable;
import jakarta.persistence.PrePersist;

import java.time.LocalDateTime;

public class AuditListener {
    @PrePersist
    public void prePersist(Object entity){
        if(entity instanceof Auditable auditable){
            LocalDateTime now = LocalDateTime.now();
            auditable.setConsumedAt(now);
        }
    }
}
