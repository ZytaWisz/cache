package com.example.audit.model;

import java.time.LocalDateTime;

public interface Auditable {
    void setConsumedAt(LocalDateTime consumedAt);
}
