package com.idp.backend.util.async;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.idp.backend.dao.LogsDao;
import com.idp.backend.dao.ServiceCatDao;
import com.idp.backend.dto.LogRequest;
import com.idp.backend.entity.ServiceCatInfo;
import com.idp.backend.mapper.LogMapper;

@Component
@Profile("!kafka")
public class DefaultLogIngestor implements AsyncIngestor<LogRequest> {

    private final LogsDao logsDao;
    private final ServiceCatDao serviceCatDao;

    public DefaultLogIngestor(LogsDao logsDao, ServiceCatDao serviceCatDao) {
        this.logsDao = logsDao;
        this.serviceCatDao = serviceCatDao;
    }

    @Override
    public void submit(LogRequest request) {
        ServiceCatInfo service = serviceCatDao.findById(request.getServiceId());
        if (service != null) {
            logsDao.saveLogs(LogMapper.toEntity(request, service));
        }
    }
}
