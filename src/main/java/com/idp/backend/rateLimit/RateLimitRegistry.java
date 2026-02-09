package com.idp.backend.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;


@Component
public class RateLimitRegistry {

    private final Map<String, TokenBucket> bucket = new ConcurrentHashMap<>();

    public TokenBucket resolveBucket(String key,RateLimitRule rule){
        return bucket.computeIfAbsent(
            key,
            k -> new TokenBucket(rule.capacity(), rule.refillRatePerSecond())
        );
    }
}
