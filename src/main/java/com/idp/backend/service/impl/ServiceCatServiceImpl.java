package com.idp.backend.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.idp.backend.audit.Auditable;
import com.idp.backend.dao.ServiceCatDao;
import com.idp.backend.dto.ServiceCatRequest;
import com.idp.backend.dto.ServiceCatResponse;
import com.idp.backend.entity.ServiceCatInfo;
import com.idp.backend.mapper.ServiceCatMapper;
import com.idp.backend.service.AuthService;
import com.idp.backend.service.ServiceCatService;
import com.idp.backend.util.SecurityUtil;
import com.idp.backend.util.SpecUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ServiceCatServiceImpl implements ServiceCatService {

    @Autowired
    private ServiceCatDao serviceDao;

    @Autowired
    private AuthService authService;

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

//    @Override
//    public void delete(UUID id) {
//        ServiceCatInfo entity= serviceDao.findById(id);
//        entity.setStatus("DEPRECATED");
//        serviceDao.save(entity);//soft-delete
//    }

    @Override
    public Page<ServiceCatResponse> listServices(String runtime, String status, String ownerTeam, Pageable pageable) {
        log.info(
                "RBAC DEBUG → user={}, roleAdmin={}, enforcedTeam={}",
                SecurityUtil.currentUsername(),
                SecurityUtil.hasRole("ADMIN"),
                SecurityUtil.currentUserTeam()
        );


//        Specification<ServiceCatInfo> spec=
//                Specification.where(ServiceSpec.hasRuntime(runtime))
//                        .and(ServiceSpec.hasStatus(status));
////                       .and(ServiceSpec.hasOwnerTeam(ownerTeam));
//        boolean isAdmin = securityUtil.isAdmin();
//        if(!isAdmin){
//            // enforce filter by the user's team, not username
//            spec=spec.and(
//                ServiceSpec.hasOwnerTeam(
//                    securityUtil.currentUserTeam()
//                )
//            );
//        } else if (ownerTeam!=null) {
//            spec = spec.and(
//                    ServiceSpec.hasOwnerTeam(ownerTeam)
//            );
//        }
//        Page<ServiceCatInfo> page= serviceDao.findAll(spec,pageable);
//        return page.map(ServiceCatMapper::toResponse);

        boolean isAd= SecurityUtil.isAdmin();

        Specification<ServiceCatInfo> spec= SpecUtil.<ServiceCatInfo>of()
            .eq("runTime",runtime)
                .eq("status",status)
                .enforceIf(!isAd,"ownerTeam",SecurityUtil.currentUserTeam())
                .build();
        return serviceDao.findAll(spec,pageable)
                .map(ServiceCatMapper::toResponse);
    }
    @Override
    @Auditable(action = "DELETE_SERVICE", resource = "SERVICE")
    public void delete(UUID id){
        ServiceCatInfo serviceCatInfo= serviceDao.findById(id);
        authService.assertCanDelete(serviceCatInfo);
        serviceDao.delete(serviceCatInfo);
    }
}
