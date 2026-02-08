package com.idp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetricRequest {

    @NotNull
    private Double cpuUsage;
    private Long memoryUsageMb;

    private String version;
    private String deployVersion;
    private String env;
}
