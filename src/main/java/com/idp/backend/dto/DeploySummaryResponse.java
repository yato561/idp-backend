package com.idp.backend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeploySummaryResponse {

    private Map<String, DeploymentEnvSummary> environments;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeploymentEnvSummary{
        private String version;
        private String status;
        private Instant deployedAt;
        private String triggeredBy;
    }

}
