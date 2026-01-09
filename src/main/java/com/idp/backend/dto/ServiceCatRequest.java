package com.idp.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCatRequest {
    @JsonProperty("serviceName")
    private String serviceName;
    @JsonProperty("repoUrl")
    private String repoUrl;
    @JsonProperty("ownerTeam")
    private String ownerTeam;
    @JsonProperty("runtime")
    private String runTime;
}
