package com.idp.backend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HealthResponse {

    private UUID serviceId;
    private Instant lastSeen;
    private boolean alive;
    private long secondsSinceLastSeen;
}
