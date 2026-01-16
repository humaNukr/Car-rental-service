package com.example.carrental.service.impl;

import com.example.carrental.dto.user.UserRegistrationRequestDto;
import com.example.carrental.dto.user.UserResponseDto;
import com.example.carrental.entity.User;
import com.example.carrental.enums.UserRole;
import com.example.carrental.mapper.user.UserMapper;
import com.example.carrental.repository.UserRepository;
import com.example.carrental.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final UserMapper mapper;

    @Override
    @Transactional
    public UserResponseDto register(UserRegistrationRequestDto request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }

        User user = mapper.toEntity(request);

        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole(UserRole.CUSTOMER);

        User savedUser = repository.save(user);

        log.info(
                "Successfully registered user with email: {}" + " and id: {}",
                savedUser.getEmail(), savedUser.getId()
        );

        return mapper.toDto(savedUser);
    }

}
