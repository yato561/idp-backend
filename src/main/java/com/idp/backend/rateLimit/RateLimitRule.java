package com.idp.backend.ratelimit;

public record RateLimitRule(String pathPrefix,int capacity, int refillRatePerSecond) {
}
