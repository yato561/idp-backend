package com.idp.backend.dao;

import com.idp.backend.entity.RefreshToken;

public interface RefreshTokenDao {
    RefreshToken save(RefreshToken token);

    RefreshToken findByToken(String token);

    void revoke(RefreshToken token);
}
