package com.idp.backend.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.idp.backend.dao.UserDao;
import com.idp.backend.entity.UserEntity;
import com.idp.backend.repo.UserRepo;

import jakarta.persistence.EntityNotFoundException;

@Repository
public class UserDaoImpl implements UserDao {

    @Autowired
    private UserRepo repo;
    @Override
    public UserEntity save(UserEntity user) {
        return repo.save(user);
    }

    @Override
    public UserEntity findByUsername(String username) {
        return repo.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
