package com.idp.backend.controller;

import com.idp.backend.dto.ServiceCatRequest;
import com.idp.backend.dto.ServiceCatResponse;
import com.idp.backend.service.ServiceCatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/services")
public class ServiceCatController {

    @Autowired
    private ServiceCatService service;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ServiceCatRequest request){
            ServiceCatResponse response= service.create(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    @GetMapping
    public ResponseEntity<?> getAll(){
            List<ServiceCatResponse> services= service.getAll();
            return ResponseEntity.ok(services);
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id){
            ServiceCatResponse response= service.getById(id);
            return ResponseEntity.ok(response);
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable UUID id,
            @RequestBody ServiceCatRequest request
    ){
            ServiceCatResponse response = service.update(id, request);
            return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
