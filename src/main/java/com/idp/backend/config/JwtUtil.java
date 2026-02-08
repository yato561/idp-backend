package com.idp.backend.config;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.idp.backend.entity.UserEntity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiry}")
    private long tokenExpiry;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(UserEntity user){
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("roles",user.getRoles())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+tokenExpiry))
                .signWith(getKey())
                .compact();
    }

    public Claims validate(String token){
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
