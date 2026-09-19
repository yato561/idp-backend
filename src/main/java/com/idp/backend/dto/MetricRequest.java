package com.idp.backend.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetricRequest {

    @NotNull
    private UUID serviceId;
    
    @NotNull
    private Double cpuUsage;
    private Long memoryUsageMb;

    private String version;
    private String deployVersion;
    private String env;
}
