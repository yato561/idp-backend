package com.idp.backend.rateLimit;

import com.idp.backend.util.SecurityUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private RateLimitRegistry registry;

    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain
    ) throws IOException, ServletException{

        String key= resolveKey(req);
        TokenBucket bucket = registry.getBucket(key);

        if(!bucket.tryConsume()){
            res.setStatus(429);
            res.getWriter().write("Rate limit exceeded");
            return;
        }

        chain.doFilter(req,res);
    }

    private String resolveKey(HttpServletRequest request) {

        String serviceId = request.getHeader("X-Service-Id");
        if( serviceId != null) return  serviceId;

        String user = SecurityUtil.currentUsername();

        if(user != null) return user;

        return request.getRemoteAddr();
    }
}
