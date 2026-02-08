package com.idp.backend.dto;

import java.time.Instant;

public interface SummaryProjection {
    Double getAvgCpu();
    Double getMaxCpu();
    Double getAvgMemory();
    Double getMaxMemory();
    Instant getLastSeen();
    String getVersion();
    String getEnv();
}

