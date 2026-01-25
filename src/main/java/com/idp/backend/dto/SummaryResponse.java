package com.idp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SummaryResponse {

    private Double avgCpu;
    private Double maxCpu;
    private Double avgMemory;
    private Double maxMemory;
    private Instant lastSeen;
    private String version;
    private String env;

    private String status;
    private Integer windowMin;

    /* ---------- FACTORY METHODS ---------- */

    public static SummaryResponse from(
            SummaryProjection p,
            String status,
            Integer window
    ) {
        return new SummaryResponse(
                p.getAvgCpu(),
                p.getMaxCpu(),
                p.getAvgMemory(),
                p.getMaxMemory(),
                p.getLastSeen(),
                p.getVersion(),
                p.getEnv(),
                status,
                window
        );
    }

    public static SummaryResponse noData(Integer window) {
        return new SummaryResponse(
                null, null, null, null,
                null, null, null,
                "NO_DATA",
                window
        );
    }
}
