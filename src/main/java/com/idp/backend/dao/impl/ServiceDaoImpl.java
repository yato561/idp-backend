package com.idp.backend.dao.impl;

import com.idp.backend.dao.ServiceCatDao;
import com.idp.backend.entity.ServiceCatInfo;
import com.idp.backend.repo.ServiceCatRepo;
import com.idp.backend.util.PaginationUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    @Override
    public ServiceCatInfo findByName(String name){ return repo.findByServiceName(name);}
    @Override
    public Page<ServiceCatInfo> findAll(Specification<ServiceCatInfo> spec, Pageable pageable) {
        return PaginationUtil.paginate(repo,spec,pageable);
    }
}
