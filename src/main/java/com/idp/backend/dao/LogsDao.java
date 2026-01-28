package com.idp.backend.dao;


import com.idp.backend.entity.LogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface LogsDao {

    void saveLogs(LogEntity entity);
    Page<LogEntity> search(Specification<LogEntity> spec, Pageable page);
}
