package com.idp.backend.service.impl;

import com.idp.backend.dao.LogsDao;
import com.idp.backend.dao.ServiceCatDao;
import com.idp.backend.dto.LogRequest;
import com.idp.backend.dto.LogResponse;
import com.idp.backend.entity.LogEntity;
import com.idp.backend.entity.ServiceCatInfo;
import com.idp.backend.mapper.LogMapper;
import com.idp.backend.service.LogsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class LogsServiceImpl implements LogsService {

    @Autowired
    private LogsDao logsDao;

    @Autowired
    private ServiceCatDao serviceCatDao;

    @Override
    public void saveLog(LogRequest request, UUID serviceId) {
        ServiceCatInfo service=  serviceCatDao.findById(serviceId);
        logsDao.saveLogs(LogMapper.toEntity(request,service));
    }

    @Override
    public Page<LogResponse> getLog(UUID serviceId, Instant from, Instant to, String level, Pageable page) {
        Specification<LogEntity> spec= (root,q,cb) -> cb.equal(root.get("serviceId"),serviceId);

        if (level!= null)
            spec= spec.and((r,q,c) -> c.equal(r.get("level"),level));

        if (from!= null)
            spec = spec.and((r,q,c) -> c.greaterThanOrEqualTo(r.get("timestamp"),from));

        if (to!= null)
            spec= spec.and((r,q,c) -> c.lessThanOrEqualTo(r.get("timestamp"),to));

        return logsDao.search(spec,page).map(LogMapper :: toResponse);
    }
}
