package com.idp.backend.controller;

import com.idp.backend.dto.DeploySummaryResponse;
import com.idp.backend.dto.DeploymentRequest;
import com.idp.backend.dto.DeploymentResponse;
import com.idp.backend.dto.RollbackRequest;
import com.idp.backend.service.DeploymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/deployments")
public class DeploymentController {

    @Autowired
    private DeploymentService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> register(@Valid @RequestBody DeploymentRequest req) {
        service.register(req);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{serviceId}")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public ResponseEntity<Page<DeploymentResponse>> history(
            @PathVariable UUID serviceId,
            Pageable page
    ) {
        return ResponseEntity.ok(service.history(serviceId, page));
    }

    @GetMapping("/{serviceId}/latest")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public ResponseEntity<DeploymentResponse> latest(@PathVariable UUID serviceId, @RequestParam(required = false) String env) {
        return ResponseEntity.ok(service.latest(serviceId,env));
    }

    @GetMapping("/{serviceId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public ResponseEntity<DeploySummaryResponse> summary(@PathVariable UUID serviceId){
        return ResponseEntity.ok(service.getSummary(serviceId));
    }

    @PostMapping("/rollback")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rollback(@Valid @RequestBody RollbackRequest request){
        service.rollback(request);
        return ResponseEntity.accepted().build();
    }
}

