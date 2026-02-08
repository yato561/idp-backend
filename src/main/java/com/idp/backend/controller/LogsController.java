package com.idp.backend.controller;


import com.idp.backend.dto.LogIngestResponse;
import com.idp.backend.dto.LogRequest;
import com.idp.backend.dto.LogResponse;
import com.idp.backend.service.LogsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/logs")
public class LogsController {

    @Autowired
    private LogsService service;

    @PostMapping("/ingest/{serviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> saveLogs(@RequestBody LogRequest request, @PathVariable UUID serviceId){
        service.saveLog(request, serviceId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{serviceId}")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public ResponseEntity<Page<LogResponse>> getLogs(@PathVariable UUID serviceId, @RequestParam(required = false)Instant from, @RequestParam(required = false) Instant to, @RequestParam(required = false) String level, Pageable page){
        return ResponseEntity.ok(service.getLog(serviceId, from, to ,level, page));
    }
}
