package com.idp.backend.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.idp.backend.dto.AuthResponse;
import com.idp.backend.dto.LoginRequest;
import com.idp.backend.dto.LogoutRequest;
import com.idp.backend.dto.RefreshRequest;
import com.idp.backend.dto.RegisterRequest;
import com.idp.backend.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {

   @Autowired
   private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest request){
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {


        return ResponseEntity.ok(
               authService.login(req)
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody RefreshRequest request
            ){
        return ResponseEntity.ok(
               authService.refresh(request)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody LogoutRequest request
    ) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
