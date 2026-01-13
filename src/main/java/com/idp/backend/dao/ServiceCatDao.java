package com.idp.backend.dao;

import com.idp.backend.entity.ServiceCatInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public interface ServiceCatDao {

    ServiceCatInfo save(ServiceCatInfo service);

    List<ServiceCatInfo> findAll();

    ServiceCatInfo findById(UUID id);

    void delete(ServiceCatInfo service);

    boolean existsByName(String name);

    Page<ServiceCatInfo> findAll(Specification<ServiceCatInfo> spec, Pageable pageable);

}
