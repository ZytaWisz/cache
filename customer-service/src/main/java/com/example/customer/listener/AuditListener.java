package com.example.customer.listener;

import com.example.customer.model.Creatable;
import com.example.customer.model.EventIdGenerator;
import com.example.customer.model.Updatable;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;
import java.util.UUID;

public class AuditListener {
    @PrePersist
    public void prePersist(Object entity){
        if(entity instanceof Creatable creatable){
            LocalDateTime now = LocalDateTime.now();
            creatable.setCreatedAt(now);
        }
        if(entity instanceof EventIdGenerator eventIdGenerator){
            eventIdGenerator.setEventId(UUID.randomUUID().toString());
        }
    }
    @PreUpdate
    public void preUpdate(Object entity){
        if(entity instanceof Updatable updatable){
            LocalDateTime now = LocalDateTime.now();
            updatable.setUpdatedAt(now);
        }
    }
}
