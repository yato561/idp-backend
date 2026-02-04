package com.idp.backend.rateLimit;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RateLimitConfig {

    private int capacity;
    private int refillPerSecond;

}
