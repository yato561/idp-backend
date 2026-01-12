package com.idp.backend.service;

import com.idp.backend.dto.AuthResponse;
import com.idp.backend.dto.LoginRequest;
import com.idp.backend.dto.LogoutRequest;
import com.idp.backend.dto.RefreshRequest;
import com.idp.backend.dto.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);

    void logout(LogoutRequest request);
}
