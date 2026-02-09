package com.idp.backend.ratelimit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class RateLimitConfig {

    @Bean
    public List<RateLimitRule> rateLimitRules(){
        return List.of(
                new RateLimitRule("/api/admin", 100,50),

                new RateLimitRule("/api/metrics/ingest",20,5),

                new RateLimitRule("/api/logs/ingest",30,10),

                new RateLimitRule("/api",60,20)
        );
    }

}
