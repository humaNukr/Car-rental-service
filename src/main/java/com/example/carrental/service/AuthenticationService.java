package com.example.carrental.service;

import com.example.carrental.dto.jwt.JwtAuthenticationDto;
import com.example.carrental.dto.user.UserLoginRequestDto;
import com.example.carrental.dto.user.UserRegistrationRequestDto;
import com.example.carrental.dto.user.UserResponseDto;
import com.example.carrental.exception.user.UserAlreadyExistsException;

public interface AuthenticationService {
    UserResponseDto register(UserRegistrationRequestDto request) throws UserAlreadyExistsException;
    JwtAuthenticationDto login(UserLoginRequestDto request);
}