package com.example.carrental.service;

import com.example.carrental.dto.user.UserResponseDto;
import com.example.carrental.dto.user.UserUpdateRequestDto;
import com.example.carrental.enums.UserRole;

public interface UserService {
    UserResponseDto updateRole(Long id, UserRole role);

    UserResponseDto getProfile();

    UserResponseDto updateProfile(UserUpdateRequestDto requestDto);
}
