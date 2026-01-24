package com.idp.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "service_metrics")
public class MetricEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID serviceId;

    @Column(nullable = false)
    private Instant timeStamp;

    private Double cpuUsage;
    private Long memoryUsageMb;

    private String version;
    private String deployVersion;
    private String env;

    @Column(nullable = false)
    private Instant receivedAt = Instant.now();
}
