package com.idp.backend.dao.impl;

import com.idp.backend.dao.ServiceCatDao;
import com.idp.backend.entity.ServiceCatInfo;
import com.idp.backend.repo.ServiceCatRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ServiceDaoImpl implements ServiceCatDao {

    @Autowired
    private ServiceCatRepo repo;

    @Override
    public ServiceCatInfo save(ServiceCatInfo service) {
        return repo.save(service);
    }

    @Override
    public List<ServiceCatInfo> findAll() {
        return repo.findAll();
    }

    @Override
    public ServiceCatInfo findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service not found with id: "+id));
    }

    @Override
    public void delete(ServiceCatInfo service) {
        repo.delete(service);
    }

    @Override
    public boolean existsByName(String name) {
        return repo.existsByServiceName(name);
    }
}
