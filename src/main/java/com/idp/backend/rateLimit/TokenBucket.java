package com.idp.backend.ratelimit;

public class TokenBucket {

    private int tokens;
    private final int capacity;
    private final int refillRate; // tokens per second
    private long lastRefillMillis;

    public TokenBucket(int capacity, int refillRate){
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = capacity;
        this.lastRefillMillis = System.currentTimeMillis();
    }

    synchronized boolean tryConsume(){
        refill();
        if(tokens > 0){
            tokens--;
            return true;
        }
        return false;
    }

    private void refill(){
        long now = System.currentTimeMillis();
        long elapsedMillis = now - lastRefillMillis;
        if(elapsedMillis <= 0 || refillRate <= 0) return;

        long refillTokens = (elapsedMillis * (long) refillRate) / 1000L;
        if(refillTokens > 0){
            tokens = Math.min(capacity, (int) (tokens + refillTokens));
            long consumedMillis = (refillTokens * 1000L) / (long) refillRate;
            lastRefillMillis += consumedMillis;
        }
    }
}
