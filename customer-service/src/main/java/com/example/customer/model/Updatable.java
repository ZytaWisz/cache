package com.example.customer.model;

import java.time.LocalDateTime;

public interface Updatable {
    void setUpdatedAt(LocalDateTime updatedAt);
}
