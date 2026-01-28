package com.example.carrental.service;

import com.example.carrental.dto.jwt.JwtAuthenticationDto;
import com.example.carrental.dto.jwt.RefreshTokenDto;
import com.example.carrental.dto.user.UserLoginRequestDto;
import com.example.carrental.dto.user.UserRegistrationRequestDto;
import com.example.carrental.dto.user.UserResponseDto;

public interface AuthenticationService {
    UserResponseDto register(UserRegistrationRequestDto request);

    JwtAuthenticationDto login(UserLoginRequestDto request);

    JwtAuthenticationDto refreshToken(RefreshTokenDto refreshTokenDto);
}