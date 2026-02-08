package com.idp.backend.service.impl;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.idp.backend.dao.RefreshTokenDao;
import com.idp.backend.entity.RefreshToken;
import com.idp.backend.entity.UserEntity;
import com.idp.backend.exception.TokenException;
import com.idp.backend.service.RefreshTokenService;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final long EXPIRY_DAYS=7;

    @Autowired
    private RefreshTokenDao refreshTokenDao;

    @Override
    public String create(UserEntity user) {
        RefreshToken refreshToken= new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(
                Instant.now().plus(EXPIRY_DAYS, ChronoUnit.DAYS)
        );
        refreshToken.setRevoked(false);
        refreshTokenDao.save(refreshToken);
        return refreshToken.getToken();
    }

    @Override
    public RefreshToken verify(String token) {

        RefreshToken refreshToken=refreshTokenDao.findByToken(token);

        if (refreshToken == null) {
            throw new TokenException("Refresh token not found");
        }

        if (refreshToken.isRevoked()){
            throw new TokenException("Refresh token revoked");
        }

        if(refreshToken.getExpiryDate().isBefore(Instant.now())){
            throw new TokenException("Refresh token expired");
        }
        return refreshToken;
    }

    @Override
    public void revoke(String token) {
        RefreshToken refreshToken = refreshTokenDao.findByToken(token);
        refreshTokenDao.revoke(refreshToken);
    }
}
