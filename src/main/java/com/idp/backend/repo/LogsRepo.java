package com.idp.backend.repo;

import com.idp.backend.entity.LogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LogsRepo extends JpaRepository<LogEntity, UUID>, JpaSpecificationExecutor<LogEntity> {
}
