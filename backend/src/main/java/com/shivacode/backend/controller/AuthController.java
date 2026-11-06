package com.shivacode.backend.controller;

import com.shivacode.backend.dto.AuthResponse;
import com.shivacode.backend.dto.LoginRequest;
import com.shivacode.backend.dto.RegisterRequest;
import com.shivacode.backend.entity.UserEntity;
import com.shivacode.backend.service.AuthService;
import com.shivacode.backend.service.JwtService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(
            AuthService authService,
            JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        UserEntity user = authService.register(request);

        String token = jwtService.generateToken(user);

        AuthResponse response = new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        UserEntity user = authService.authenticate(request);

        String token = jwtService.generateToken(user);

        AuthResponse response = new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );

        return ResponseEntity.ok(response);
    }
}