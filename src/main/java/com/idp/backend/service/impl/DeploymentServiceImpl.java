package com.idp.backend.service.impl;

import com.idp.backend.dao.DeploymentDao;
import com.idp.backend.dto.DeploymentRequest;
import com.idp.backend.dto.DeploymentResponse;
import com.idp.backend.entity.DeploymentEntity;
import com.idp.backend.mapper.DeploymentMapper;
import com.idp.backend.service.DeploymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
public class DeploymentServiceImpl implements DeploymentService {

    @Autowired
    private DeploymentDao dao;
    @Override
    public void register(DeploymentRequest request) {
        dao.save(DeploymentMapper.toEntity(request));
    }

    @Override
    public Page<DeploymentResponse> history(UUID serviceId, Pageable page) {
        return dao.findByService(serviceId, page).map(DeploymentMapper :: toResponse);
    }

    @Override
    public DeploymentResponse latest(UUID serviceId) {
        DeploymentEntity entity= dao.latest(serviceId);
        return entity == null ? null : DeploymentMapper.toResponse(entity);
    }
}
