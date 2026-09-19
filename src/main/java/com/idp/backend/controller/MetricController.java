package com.idp.backend.controller;


import com.idp.backend.dto.HealthResponse;
import com.idp.backend.dto.MetricRequest;
import com.idp.backend.dto.MetricResponse;
import com.idp.backend.dto.SummaryResponse;
import com.idp.backend.service.MetricService;
import com.idp.backend.util.PrometheusClient;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/metrics")
public class MetricController {

    @Autowired
    public MetricService service;

    @Autowired
    public PrometheusClient prometheusClient;

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

    @GetMapping("/{serviceId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'VIEWER')")
    public ResponseEntity<SummaryResponse> summarize(@PathVariable UUID serviceId, @RequestParam(required = false) Integer window,
                                                     @RequestParam(required = false)Instant from, @RequestParam(required = false) Instant to){
            return ResponseEntity.ok(service.getSummaryById(serviceId, window, from, to));
    }

    /**
     * Sync metrics from Prometheus for a specific service
     * Pulls current metrics from Prometheus and ingests them into the system
     */
    @PostMapping("/prometheus/sync/{serviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> syncFromPrometheus(@PathVariable UUID serviceId, @RequestParam String serviceName){
        service.syncFromPrometheus(serviceId, serviceName);
        return ResponseEntity.accepted().build();
    }

    /**
     * Pull all services metrics from Prometheus
     * Bulk operation to sync metrics for all registered services
     */
    @PostMapping("/prometheus/pull-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> pullAllFromPrometheus(){
        service.pullAllMetricsFromPrometheus();
        return ResponseEntity.accepted().build();
    }

    /**
     * Get raw Prometheus metrics query result
     * For advanced queries and debugging
     */
    @GetMapping("/prometheus/query")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> queryPrometheus(@RequestParam String query){
        return ResponseEntity.ok(prometheusClient.query(query));
    }
}
