package com.idp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCatResponse {
    private UUID serviceId;
    private String serviceName;
    private String repoUrl;
    private String ownerTeam;
    private String runTime;
    private String status;
    private LocalDateTime createdAt;
}
