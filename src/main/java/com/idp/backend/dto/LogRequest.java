package com.idp.backend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogRequest {

    private UUID serviceId;
    private Instant timeStamp;
    private String level;
    private String message;
    private String traceId;
    private String source;
}
