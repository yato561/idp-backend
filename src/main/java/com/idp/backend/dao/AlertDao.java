package com.idp.backend.dao;

import com.idp.backend.entity.AlertEntity;

import java.util.List;
import java.util.UUID;

public interface AlertDao {

    AlertEntity save(AlertEntity alert);

    List<AlertEntity> findOpen();

    List<AlertEntity> findByService(UUID serviceId);

    AlertEntity findById(UUID id);

    boolean hasOpenAlert(UUID serviceId, String type);
}
