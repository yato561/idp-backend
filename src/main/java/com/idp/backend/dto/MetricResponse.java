package com.idp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetricResponse {

    private UUID id;
    private Instant timestamp;
    private Double cpuUsage;
    private Long memoryUsageMb;
    private String version;
    private String deployVersion;
    private String env;
}
