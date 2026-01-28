package com.idp.backend.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_logs",
        indexes = {
                @Index(name = "idx_logs_service_time", columnList = "serviceId,timestamp"),
                @Index(name = "idx_logs_trace", columnList = "traceId")
        })
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogEntity {

    @Id
    @GeneratedValue
    private UUID logId;

    private UUID serviceId;

    private Instant timeStamp;

    private String level;

    private String message;

    private String traceId;

    private String source;

}
