package com.idp.backend.dao;

import com.idp.backend.entity.UserEntity;

public interface UserDao {

    UserEntity save(UserEntity user);
    UserEntity findByUsername(String username);
}
