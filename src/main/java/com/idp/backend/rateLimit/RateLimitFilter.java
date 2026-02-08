package com.idp.backend.ratelimit;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.idp.backend.util.SecurityUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitRegistry registry;
    private final Map<String, RateLimitRule> rules;

    public RateLimitFilter(RateLimitRegistry registry, Map<String, RateLimitRule> rules) {
        this.registry = registry;
        this.rules = rules;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain
    ) throws IOException, ServletException{


        if(!"POST".equalsIgnoreCase(req.getMethod())){
            chain.doFilter(req,res);
            return;
        }

        String path = req.getRequestURI();

        String identifier = resolveKey(req);

        String role;
        if (req.getHeader("X-Service-Id") != null) {
            role = "SERVICE";
        } else if (SecurityUtil.isAdmin()) {
            role = "ADMIN";
        } else {
            role = "VIEWER";
        }

        String ruleKey = role + ":" + req.getMethod().toUpperCase() + ":" + path;
        RateLimitRule rule = rules.get(ruleKey);

        if (rule == null) {
            chain.doFilter(req, res);
            return;
        }

        String bucketKey = ruleKey + ":" + identifier;
        TokenBucket bucket = registry.getBucket(bucketKey, rule.capacity(), rule.refillRatePerSecond());

        if (!bucket.tryConsume()) {
            res.setStatus(429);
            res.setContentType("text/plain");
            res.getWriter().write("Rate limit exceeded");
            return;
        }

        chain.doFilter(req, res);
    }

    private String resolveKey(HttpServletRequest request) {

        String serviceId = request.getHeader("X-Service-Id");
        if( serviceId != null) return  serviceId;

        String user = SecurityUtil.currentUsername();

        if(user != null) return user;

        return request.getRemoteAddr();
    }
}
