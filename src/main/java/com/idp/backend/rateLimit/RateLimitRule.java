package com.idp.backend.ratelimit;

public record RateLimitRule(int capacity, int refillRatePerSecond) {
}
