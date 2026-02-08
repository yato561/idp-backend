package com.idp.backend.service;

import com.idp.backend.entity.RefreshToken;
import com.idp.backend.entity.UserEntity;

public interface RefreshTokenService {

    String create(UserEntity user);

    RefreshToken verify(String token);

    void revoke(String token);
}
