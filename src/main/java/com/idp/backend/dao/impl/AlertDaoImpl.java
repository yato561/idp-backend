package com.idp.backend.dao.impl;

import com.idp.backend.dao.AlertDao;
import com.idp.backend.entity.AlertEntity;
import com.idp.backend.repo.AlertRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public class AlertDaoImpl implements AlertDao {


    @Autowired
    private AlertRepo repo;

    @Override
    public AlertEntity save(AlertEntity alert) {
        return repo.save(alert);
    }

    @Override
    public List<AlertEntity> findOpen() {
        return repo.findByStatus("OPEN");
    }

    @Override
    public List<AlertEntity> findByService(UUID serviceId) {
        return repo.findByServiceId(serviceId);
    }

    @Override
    public AlertEntity findById(UUID id) {
        return repo.findById(id).orElseThrow();
    }

    @Override
    public boolean hasOpenAlert(UUID serviceId, String type) {
        return repo.existsByServiceIdAndTypeAndStatus(serviceId, type, "OPEN");
    }
}
