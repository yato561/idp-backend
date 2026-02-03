package com.idp.backend.repo;


import com.idp.backend.entity.AlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepo extends JpaRepository<AlertEntity, UUID> {

    List<AlertEntity> findByStatus(String status);

    List<AlertEntity> findByServiceId(UUID serviceId);

    boolean existsByServiceIdAndTypeAndStatus(UUID serviceId, String type, String status);
}
