package com.example.carrental.service.impl;

import com.example.carrental.dto.user.UserResponseDto;
import com.example.carrental.dto.user.UserRoleUpdateDto;
import com.example.carrental.dto.user.UserUpdateRequestDto;
import com.example.carrental.entity.User;
import com.example.carrental.exception.base.EntityNotFoundException;
import com.example.carrental.mapper.user.UserMapper;
import com.example.carrental.repository.UserRepository;
import com.example.carrental.security.SecurityFacade;
import com.example.carrental.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;
    private final SecurityFacade securityFacade;

    @Override
    @Transactional
    public UserResponseDto updateRole(Long id, UserRoleUpdateDto requestDto) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        User currentUser = securityFacade.getCurrentUser();

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Action denied: You cannot change your own role.");
        }

        targetUser.setRole(requestDto.getRole());
        return mapper.toDto(userRepository.save(targetUser));
    }

    @Override
    public UserResponseDto getProfile() {
        User user = securityFacade.getCurrentUser();
        return mapper.toDto(user);
    }

    @Override
    @Transactional
    public UserResponseDto updateProfile(UserUpdateRequestDto requestDto) {
        User user = securityFacade.getCurrentUser();
        mapper.updateUserFromDto(requestDto, user);

        User savedUser = userRepository.save(user);
        return mapper.toDto(savedUser);
    }
}
