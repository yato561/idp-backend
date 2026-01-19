package com.idp.backend.service;

import com.idp.backend.dto.ServiceCatRequest;
import com.idp.backend.dto.ServiceCatResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.UUID;

public interface ServiceCatService {

    ServiceCatResponse create(ServiceCatRequest request);

    List<ServiceCatResponse> getAll();

    ServiceCatResponse getById(UUID id);

    ServiceCatResponse update(UUID id, ServiceCatRequest request) throws AccessDeniedException;

    void delete(UUID id) throws AccessDeniedException;

    Page<ServiceCatResponse> listServices(String runtime, String status, String ownerTeam, Pageable pageable);
}
