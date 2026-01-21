package com.example.carrental.controller;

import com.example.carrental.dto.jwt.JwtAuthenticationDto;
import com.example.carrental.dto.jwt.RefreshTokenDto;
import com.example.carrental.dto.user.UserLoginRequestDto;
import com.example.carrental.dto.user.UserRegistrationRequestDto;
import com.example.carrental.dto.user.UserResponseDto;
import com.example.carrental.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public UserResponseDto register(@RequestBody @Valid UserRegistrationRequestDto request) {
        return authenticationService.register(request);
    }

    @PostMapping("/login")
    public JwtAuthenticationDto login(@RequestBody @Valid UserLoginRequestDto request) {
        return authenticationService.login(request);
    }

    @PostMapping("/refresh")
    public JwtAuthenticationDto refreshToken(@RequestBody @Valid RefreshTokenDto refreshTokenDto) {
        return authenticationService.refreshToken(refreshTokenDto);
    }
}
