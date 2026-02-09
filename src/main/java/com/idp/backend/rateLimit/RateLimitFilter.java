package com.idp.backend.ratelimit;

import java.io.IOException;
import java.util.List;
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
    private final List<RateLimitRule> rules;

    public RateLimitFilter(RateLimitRegistry registry, List<RateLimitRule> rules) {
        this.registry = registry;
        this.rules = rules;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain
    ) throws IOException, ServletException{

        String path = req.getRequestURI();

        RateLimitRule rule= rules.stream()
                .filter(r -> path.startsWith(r.pathPrefix()))
                .findFirst()
                .orElse(null);
        if (rule != null && SecurityUtil.isAuthenticated()){

            String username = SecurityUtil.currentUsername();
            String role=SecurityUtil.isAdmin()?"ADMIN" : "VIEWER";

            String bucketKey= role + ":" +username+":"+rule.pathPrefix();

            TokenBucket bucket= registry.resolveBucket(bucketKey, rule);

            if(!bucket.tryConsume()){
                res.setStatus(429);
                res.getWriter().write("Rate limit exceeded");
                return;
            }

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
