package com.idp.backend.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table( name = "alerts",
        indexes = {
        @Index(name = "idx_alert_service_status", columnList = "serviceId,status"),
        @Index(name = "idx_alert_triggered_at", columnList = "triggeredAt")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID serviceId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant triggeredAt= Instant.now();

    private Instant resolvedAt;

    private String message;

}
