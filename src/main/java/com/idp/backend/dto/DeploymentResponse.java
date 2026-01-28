package com.idp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeploymentResponse {

    private UUID deployId;
    private String version;
    private String env;
    private String status;
    private Instant deployedAt;
    private String triggeredBy;
}
