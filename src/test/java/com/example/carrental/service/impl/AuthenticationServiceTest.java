package com.example.carrental.service.impl;

import com.example.carrental.dto.jwt.JwtAuthenticationDto;
import com.example.carrental.dto.user.UserLoginRequestDto;
import com.example.carrental.dto.user.UserRegistrationRequestDto;
import com.example.carrental.dto.user.UserResponseDto;
import com.example.carrental.entity.User;
import com.example.carrental.enums.UserRole;
import com.example.carrental.mapper.user.UserMapper;
import com.example.carrental.repository.UserRepository;
import com.example.carrental.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class AuthenticationServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private UserRegistrationRequestDto defaultRequest;

    @BeforeEach
    void setUp() {
        defaultRequest = new UserRegistrationRequestDto();
        defaultRequest.setEmail("email@email");
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
        when(userRepository.findByEmail(defaultRequest.getEmail())).thenReturn(Optional.empty());
        when(userMapper.toDto(savedUser)).thenReturn(response);
        when(userMapper.toEntity(defaultRequest)).thenReturn(user);

        UserResponseDto result = authenticationService.register(defaultRequest);

        assertEquals(result, response);

        verify(passwordEncoder).encode("password");
    }

    @Test
    @DisplayName("Should throw exception if email exists")
    void shouldThrowExceptionIfEmailExists() {
        when(userRepository.findByEmail(defaultRequest.getEmail())).thenReturn(Optional.of(new User()));

        assertThrows(Exception.class, () -> authenticationService.register(defaultRequest));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Verify that login works correctly")
    void verifyLoginWorksCorrectly() {
        UserLoginRequestDto loginRequest = new UserLoginRequestDto();
        loginRequest.setEmail("email@email");
        loginRequest.setPassword("password");

        Authentication auth = new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtil.generateAccessToken(loginRequest.getEmail())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(loginRequest.getEmail())).thenReturn("refresh-token");

        JwtAuthenticationDto result = authenticationService.login(loginRequest);

        assertNotNull(result);
        assertEquals("access-token", result.getToken());
        assertEquals("refresh-token", result.getRefreshToken());

    }

    @Test
    @DisplayName("Should throw exception if password is incorrect")
    void shouldThrowExceptionIfPasswordIsIncorrect() {
        UserLoginRequestDto loginRequest = new UserLoginRequestDto();
        loginRequest.setEmail("email@email");
        loginRequest.setPassword("password");

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(AuthenticationException.class, () -> authenticationService.login(loginRequest));
        verify(jwtUtil, never()).generateAccessToken(loginRequest.getEmail());
        verify(jwtUtil, never()).generateRefreshToken(loginRequest.getEmail());
    }

}