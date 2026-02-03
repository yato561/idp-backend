package com.idp.backend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlertResponse {

    private UUID id;
    private UUID serviceId;
    private String type;
    private String severity;
    private String status;
    private Instant triggeredAt;
    private Instant resolvedAt;
    private String message;
}
