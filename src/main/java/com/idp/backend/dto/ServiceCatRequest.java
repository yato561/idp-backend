package com.idp.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCatRequest {
    @NotBlank(message = "Service name is required")
    @JsonProperty("serviceName")
    private String serviceName;
    @JsonProperty("repoUrl")
    private String repoUrl;
    @JsonProperty("ownerTeam")
    private String ownerTeam;
    @JsonProperty("runtime")
    private String runTime;
}
