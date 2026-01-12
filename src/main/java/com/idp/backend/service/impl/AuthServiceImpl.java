package com.idp.backend.service.impl;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.idp.backend.config.JwtUtil;
import com.idp.backend.dao.UserDao;
import com.idp.backend.dto.AuthResponse;
import com.idp.backend.dto.LoginRequest;
import com.idp.backend.dto.LogoutRequest;
import com.idp.backend.dto.RefreshRequest;
import com.idp.backend.dto.RegisterRequest;
import com.idp.backend.entity.RefreshToken;
import com.idp.backend.entity.UserEntity;
import com.idp.backend.service.AuthService;
import com.idp.backend.service.RefreshTokenService;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private UserDao userDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Override
    public void register(RegisterRequest request) {
        UserEntity user= new UserEntity();

        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of("ROLE_VIEWER"));

        userDao.save(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        if (request.getUsername() ==null || request.getPassword()==null){
            throw new BadCredentialsException("Credentials Missing");
        }
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserEntity user = userDao.findByUsername(request.getUsername());

        return new AuthResponse(
                jwtUtil.generateToken(user),
                refreshTokenService.create(user)
        );
    }

    @Override
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken token= refreshTokenService.verify(request.getRefreshToken());

        return new AuthResponse(
                jwtUtil.generateToken(token.getUser()),
                token.getToken()
        );
    }

    @Override
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
    }
}
