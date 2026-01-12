package com.idp.backend.config;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(JwtFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        logger.info(">>> JwtFilter START for {}", request.getRequestURI());

        String header = request.getHeader("Authorization");
        logger.info("Authorization header = {}", header);

        try {
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                logger.info("Validating token: {}", token);
                Claims claims = jwtUtil.validate(token);

                List<GrantedAuthority> authorities = new ArrayList<>();
                List<?> roles = (List<?>) claims.get("roles");

                if (roles != null && !roles.isEmpty()) {
                    for (Object role : roles) {
                        if (role != null) {
                            authorities.add(
                                    new SimpleGrantedAuthority(
                                            role.toString().startsWith("ROLE_")
                                                    ? role.toString()
                                                    : "ROLE_" + role.toString()
                                    )
                            );
                        }
                    }
                }
                logger.info("Authorities extracted: " + authorities);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                claims.getSubject(),
                                null,
                                authorities
                        );
                auth.setDetails(claims);

                SecurityContextHolder.getContext().setAuthentication(auth);
                logger.info("JWT validated for user: " + claims.getSubject());
            }
        } catch (JwtException e) {
            logger.error("JWT validation failed: " + e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            logger.error("Error processing JWT: " + e.getMessage());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
