package com.idp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeploymentRequest {

    @NotNull
    private UUID serviceId;

    @NotBlank
    private String version;

    @NotBlank
    private String env;

    @NotBlank
    private String status;

    private String triggeredBy;
}
