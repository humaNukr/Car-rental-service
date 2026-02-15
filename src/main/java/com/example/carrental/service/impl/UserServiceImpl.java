package com.example.carrental.service.impl;

import com.example.carrental.dto.user.UserResponseDto;
import com.example.carrental.dto.user.UserRoleUpdateDto;
import com.example.carrental.dto.user.UserUpdateRequestDto;
import com.example.carrental.entity.User;
import com.example.carrental.enums.UserRole;
import com.example.carrental.exception.base.EntityNotFoundException;
import com.example.carrental.mapper.user.UserMapper;
import com.example.carrental.repository.UserRepository;
import com.example.carrental.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;

    @Override
    @Transactional
    public UserResponseDto updateRole(Long id, UserRoleUpdateDto requestDto) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = authentication.getName();

        if (targetUser.getEmail().equals(currentEmail)) {
            throw new RuntimeException("Action denied: You cannot change your own role.");
        }

        targetUser.setRole(requestDto.getRole());
        return mapper.toDto(userRepository.save(targetUser));
    }

    @Override
    public UserResponseDto getProfile() {
        User user = getCurrentUser();
        return mapper.toDto(user);
    }

    @Override
    public UserResponseDto updateProfile(UserUpdateRequestDto requestDto) {
        User user = getCurrentUser();
        mapper.updateUserFromDto(requestDto, user);

        User savedUser = userRepository.save(user);
        return mapper.toDto(savedUser);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found with email: " + email));
    }
}
