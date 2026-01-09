package com.idp.backend.controller;

import com.idp.backend.dto.ServiceCatRequest;
import com.idp.backend.dto.ServiceCatResponse;
import com.idp.backend.service.ServiceCatService;
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
    public ResponseEntity<?> create(@RequestBody ServiceCatRequest request){
        try{
            ServiceCatResponse response= service.create(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }catch (Exception e) {
            return new ResponseEntity<>(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
    @GetMapping
    public ResponseEntity<?> getAll(){
        try{
            List<ServiceCatResponse> services= service.getAll();
            return ResponseEntity.ok(services);
        }catch (Exception e){
            return new ResponseEntity<>(
                    "Failed to fetch services",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id){
        try{
            ServiceCatResponse response= service.getById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    "Service not found",
                    HttpStatus.NOT_FOUND
            );
        }
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable UUID id,
            @RequestBody ServiceCatRequest request
    ){
        try{
            ServiceCatResponse response = service.update(id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id){
        try{
            service.delete(id);
            return new ResponseEntity<>(
                    "Service Deprecated Successfully",
                    HttpStatus.OK
            );
        }catch (Exception e){
            return new ResponseEntity<>(
                    "Failed to delete service",
                    HttpStatus.NOT_FOUND
            );
        }
    }
}
