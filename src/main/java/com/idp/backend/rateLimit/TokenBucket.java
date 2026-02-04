package com.idp.backend.rateLimit;

import java.time.Duration;
import java.time.Instant;

public class TokenBucket {

    private int tokens;
    private final int capacity;
    private final int refillRate;
    private Instant lastRefill;

    public TokenBucket(int capacity, int refillRate){
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = capacity;
        this.lastRefill= Instant.now();

    }

    synchronized boolean tryConsume(){
        refill();
        if(tokens >0){
            tokens--;
            return true;
        }
        return false;
    }

    private void refill(){
        Instant now= Instant.now();
        long seconds= Duration.between(lastRefill,now).getSeconds();
        if(seconds >0){
            int refill = (int) (seconds* refillRate);
            tokens = Math.min(capacity, tokens + refill);
            lastRefill = now;
        }
    }
}
