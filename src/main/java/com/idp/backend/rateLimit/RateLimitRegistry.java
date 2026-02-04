package com.idp.backend.rateLimit;


import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RateLimitRegistry {

    private final Map<String, TokenBucket> bucket = new ConcurrentHashMap<>();

    public TokenBucket getBucket(String key){

        return bucket.computeIfAbsent(
                key,
                k -> new TokenBucket(20, 5)
        );
    }
}
