package com.idp.backend.ratelimit;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class RateLimitConfig {

    @Bean
    public Map<String, RateLimitRule> rateLimitRules(){

        Map<String, RateLimitRule> rules = new HashMap<>();

        rules.put("ADMIN:POST:/api/deployments", new RateLimitRule(100,10));

        rules.put("ADMIN:POST:/api/logs/ingest", new RateLimitRule(200,20));

        rules.put("SERVICE:POST:/api/metrics/ingest", new RateLimitRule(60, 5));

        rules.put("SERVICE:POST:/api/alerts/evaluate", new RateLimitRule(30,2));

        return rules;
    }

}
