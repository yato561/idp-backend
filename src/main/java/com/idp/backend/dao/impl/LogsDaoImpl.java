package com.idp.backend.dao.impl;

import com.idp.backend.dao.LogsDao;
import com.idp.backend.entity.LogEntity;
import com.idp.backend.repo.LogsRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class LogsDaoImpl implements LogsDao {

    @Autowired
    private LogsRepo repo;
    @Override
    public void saveLogs(LogEntity entity) {
        repo.save(entity);
    }

    @Override
    public Page<LogEntity> search(Specification<LogEntity> spec, Pageable page) {
        return repo.findAll(spec, page);
    }
}
