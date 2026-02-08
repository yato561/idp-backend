package com.idp.backend.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.idp.backend.dao.UserDao;
import com.idp.backend.entity.UserEntity;


@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserDao userDao;

    @Override
    public UserDetails loadUserByUsername(String username){

        UserEntity user= userDao.findByUsername(username);

        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(
                        user.getRoles()
                                .stream()
                                .map(SimpleGrantedAuthority::new)
                                .toList()
                ).build();
    }
}
