package com.idp.backend.service;

import com.idp.backend.dto.DeploySummaryResponse;
import com.idp.backend.dto.DeploymentRequest;
import com.idp.backend.dto.DeploymentResponse;
import com.idp.backend.dto.RollbackRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DeploymentService {
    void register(DeploymentRequest request);

    Page<DeploymentResponse> history(UUID serviceId, Pageable page);

    DeploymentResponse latest(UUID serviceId, String env);

    DeploySummaryResponse getSummary(UUID serviceId);

    void rollback(RollbackRequest request);
 }
