package com.library.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.library.dto.request.LoginRequest;
import com.library.dto.response.TokenResponse;
import com.library.entity.User;
import com.library.repository.UserRepository;
import com.library.security.JwtService;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository users;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository users, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.jwtService = jwtService;
    }

    public TokenResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = users.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));
        return new TokenResponse(jwtService.createToken(user.getEmail(), user.getRole()),
                user.getEmail(), user.getRole().name());
    }
}
