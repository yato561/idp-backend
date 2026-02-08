package com.idp.backend.service;

import com.idp.backend.dto.HealthResponse;
import com.idp.backend.dto.MetricRequest;
import com.idp.backend.dto.MetricResponse;
import com.idp.backend.dto.SummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface MetricService {

    void ingest(UUID serviceId,MetricRequest request);
    void heartbeat(UUID serviceId);
    MetricResponse latest(UUID serviceId);
    Page<MetricResponse> history(UUID serviceId, Pageable page);
    HealthResponse getHealth(UUID serviceId);
    SummaryResponse getSummaryById(UUID serviceId, Integer window, Instant from, Instant to);

}
