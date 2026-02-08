package com.idp.backend.service;

import com.idp.backend.dto.LogIngestResponse;
import com.idp.backend.dto.LogRequest;
import com.idp.backend.dto.LogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface LogsService {

    void saveLog(LogRequest request, UUID serviceId);
    Page<LogResponse> getLog(UUID serviceId, Instant from, Instant to, String level, Pageable page);
}
