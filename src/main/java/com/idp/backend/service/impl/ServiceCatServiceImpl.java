package com.idp.backend.service.impl;

import com.idp.backend.dao.ServiceCatDao;
import com.idp.backend.dto.ServiceCatRequest;
import com.idp.backend.dto.ServiceCatResponse;
import com.idp.backend.entity.ServiceCatInfo;
import com.idp.backend.mapper.ServiceCatMapper;
import com.idp.backend.service.ServiceCatService;
import com.idp.backend.util.PaginationUtil;
import com.idp.backend.util.SecurityUtil;
import com.idp.backend.util.ServiceSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
public class ServiceCatServiceImpl implements ServiceCatService {

    @Autowired
    private ServiceCatDao serviceDao;

    @Override
    public ServiceCatResponse create(ServiceCatRequest request) {
        System.out.println("REQUEST serviceName = " + request.getServiceName());
        if(serviceDao.existsByName(request.getServiceName())){
            throw new RuntimeException("Service already exists");
        }
        ServiceCatInfo saved= serviceDao.save(ServiceCatMapper.toEntity(request));
        System.out.println("ENTITY serviceName = " + saved.getServiceName());
        return ServiceCatMapper.toResponse(saved);
    }

    @Override
    public List<ServiceCatResponse> getAll() {
        return serviceDao.findAll()
                .stream()
                .map(ServiceCatMapper::toResponse)
                .toList();
    }

    @Override
    public ServiceCatResponse getById(UUID id) {
        return ServiceCatMapper.toResponse(serviceDao.findById(id));
    }

    @Override
    public ServiceCatResponse update(UUID id, ServiceCatRequest request) {
        ServiceCatInfo entity= serviceDao.findById(id);
        entity.setRepoUrl(request.getRepoUrl());
        entity.setOwnerTeam(request.getOwnerTeam());
        entity.setRunTime(request.getRunTime());
        return ServiceCatMapper.toResponse(serviceDao.save(entity));
    }

    @Override
    public void delete(UUID id) {
        ServiceCatInfo entity= serviceDao.findById(id);
        entity.setStatus("DEPRECATED");
        serviceDao.save(entity);//soft-delete
    }

    @Override
    public Page<ServiceCatResponse> listServices(String runtime, String status, String ownerTeam, Pageable pageable) {
        Specification<ServiceCatInfo> spec=
                Specification.where(ServiceSpec.hasRuntime(runtime))
                        .and(ServiceSpec.hasStatus(status));
//                       .and(ServiceSpec.hasOwnerTeam(ownerTeam));
        if(!SecurityUtil.hasRole("ADMIN")){
            spec=spec.and(
                    ServiceSpec.hasOwnerTeam(
                            SecurityUtil.currentUsername()
                    )
            );
        }
        Page<ServiceCatInfo> page= serviceDao.findAll(spec,pageable);
        return page.map(ServiceCatMapper::toResponse);
    }

}
