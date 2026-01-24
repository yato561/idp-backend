package com.idp.backend.controller;


import com.idp.backend.dto.HealthResponse;
import com.idp.backend.dto.MetricRequest;
import com.idp.backend.dto.MetricResponse;
import com.idp.backend.service.MetricService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/metrics")
public class MetricController {

    @Autowired
    public MetricService service;

    @PostMapping("/ingest/{serviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> ingest(@PathVariable UUID serviceId, @Valid @RequestBody MetricRequest request){
        service.ingest(serviceId,request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/heartbeat/{serviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> heartbeat(@PathVariable UUID serviceId){
        service.heartbeat(serviceId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{serviceId}/latest")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public ResponseEntity<MetricResponse> latest(@PathVariable UUID serviceId){
        return ResponseEntity.ok(service.latest(serviceId));
    }

    @GetMapping("/{serviceId}/health")
    public ResponseEntity<HealthResponse> health(@PathVariable UUID serviceId){
        return ResponseEntity.ok(service.getHealth(serviceId));
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<Page<MetricResponse>> history(@PathVariable UUID serviceId, Pageable page){
        return ResponseEntity.ok(service.history(serviceId,page));
    }
}
