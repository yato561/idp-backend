package com.idp.backend.controller;


import com.idp.backend.dto.AlertResponse;
import com.idp.backend.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    @Autowired
    private AlertService service;

    @PostMapping("/evaluate/{serviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> evaluate(@PathVariable UUID serviceId){
        service.evaluate(serviceId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public ResponseEntity<List<AlertResponse>> all(){
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{serviceId}")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public ResponseEntity<List<AlertResponse>> byService(@PathVariable UUID serviceId){
        return ResponseEntity.ok(service.getByService(serviceId));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public ResponseEntity<Void> resolve(@PathVariable UUID id){
        service.resolve(id);
        return ResponseEntity.ok().build();
    }
}
