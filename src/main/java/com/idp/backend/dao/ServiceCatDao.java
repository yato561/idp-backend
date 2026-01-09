package com.idp.backend.dao;

import com.idp.backend.entity.ServiceCatInfo;

import java.util.List;
import java.util.UUID;

public interface ServiceCatDao {

    ServiceCatInfo save(ServiceCatInfo service);

    List<ServiceCatInfo> findAll();

    ServiceCatInfo findById(UUID id);

    void delete(ServiceCatInfo service);

    boolean existsByName(String name);

}
