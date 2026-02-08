package com.idp.backend.dao.impl;

import com.idp.backend.dao.RefreshTokenDao;
import com.idp.backend.entity.RefreshToken;
import com.idp.backend.repo.RefreshTokenRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenDaoImpl implements RefreshTokenDao {

    @Autowired
    private RefreshTokenRepo repo;

    @Override
    public RefreshToken save(RefreshToken token) {
        return repo.save(token);
    }

    @Override
    public RefreshToken findByToken(String token) {
        return repo.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Refresh token not found"));
    }

    @Override
    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        repo.save(token);
    }
}
