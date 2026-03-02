package com.example.carrental.service.interfaces;

import com.example.carrental.dto.user.UserResponseDto;
import com.example.carrental.dto.user.UserRoleUpdateDto;
import com.example.carrental.dto.user.UserUpdateRequestDto;

public interface UserService {
    UserResponseDto updateRole(Long id, UserRoleUpdateDto requestDto);

    UserResponseDto getProfile();

    UserResponseDto updateProfile(UserUpdateRequestDto requestDto);
}
