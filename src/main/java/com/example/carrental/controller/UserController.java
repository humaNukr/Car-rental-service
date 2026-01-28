package com.example.carrental.controller;

import com.example.carrental.dto.user.UserResponseDto;
import com.example.carrental.dto.user.UserRoleUpdateDto;
import com.example.carrental.dto.user.UserUpdateRequestDto;
import com.example.carrental.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponseDto getMyProfile() {
        return userService.getProfile();
    }

    @PatchMapping("/me")
    public UserResponseDto updateMyProfile(@RequestBody UserUpdateRequestDto requestDto) {
        return userService.updateProfile(requestDto);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDto updateUserRole(@PathVariable Long id, @RequestBody UserRoleUpdateDto requestDto) {
        return userService.updateRole(id, requestDto.getRole());
    }
}
