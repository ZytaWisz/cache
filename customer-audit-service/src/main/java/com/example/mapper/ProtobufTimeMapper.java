package com.example.mapper;

import com.google.protobuf.Timestamp;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class ProtobufTimeMapper {

    public LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(
                        timestamp.getSeconds(),
                        timestamp.getNanos()
                ),
                ZoneId.systemDefault()
        );
    }
}
