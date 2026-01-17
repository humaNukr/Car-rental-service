package com.example.carrental.service.impl;

import com.example.carrental.dto.user.UserRegistrationRequestDto;
import com.example.carrental.dto.user.UserResponseDto;
import com.example.carrental.entity.User;
import com.example.carrental.enums.UserRole;
import com.example.carrental.mapper.user.UserMapper;
import com.example.carrental.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegistrationRequestDto defaultRequest;

    @BeforeEach
    void setUp() {
        defaultRequest = new UserRegistrationRequestDto();
        defaultRequest.setEmail("test@test.com");
        defaultRequest.setPassword("password");
    }

    @Test
    @DisplayName("Verify that registration works correctly")
    void verifyRegistrationWorksCorrectly() {
        User user = new User();
        user.setEmail("email@email");
        user.setPassword("password");

        User savedUser = new User();
        savedUser.setPassword("password");
        savedUser.setEmail("email@email");
        savedUser.setId(1L);
        savedUser.setRole(UserRole.CUSTOMER);

        UserResponseDto response = new UserResponseDto();
        response.setId(1L);
        response.setEmail("email@email");
        response.setRole("CUSTOMER");

        when(userRepository.save(user)).thenReturn(savedUser);
        when(passwordEncoder.encode(defaultRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.findByEmail("email@email")).thenReturn(Optional.empty());
        when(userMapper.toDto(savedUser)).thenReturn(response);
        when(userMapper.toEntity(defaultRequest)).thenReturn(user);

        UserResponseDto result = userService.register(defaultRequest);

        assertEquals(result, response);

        verify(passwordEncoder).encode("password");
    }

    @Test
    @DisplayName("Should throw exception if email exists")
    void shouldThrowExceptionIfEmailExists() {
        when(userRepository.findByEmail("email@email")).thenReturn(Optional.of(new User()));

        assertThrows(Exception.class, () -> userService.register(defaultRequest));

        verify(userRepository, never()).save(any());
    }
}