package com.idp.backend.service;

import com.idp.backend.dto.AlertResponse;

import java.util.List;
import java.util.UUID;

public interface AlertService {

    void evaluate(UUID serviceId);

    List<AlertResponse> getAll();

    List<AlertResponse> getByService(UUID serviceId);

    void resolve(UUID alertId);
}
