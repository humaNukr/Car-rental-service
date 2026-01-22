package com.example.carrental.service.impl;

import com.example.carrental.dto.jwt.JwtAuthenticationDto;
import com.example.carrental.dto.jwt.RefreshTokenDto;
import com.example.carrental.dto.user.UserLoginRequestDto;
import com.example.carrental.dto.user.UserRegistrationRequestDto;
import com.example.carrental.dto.user.UserResponseDto;
import com.example.carrental.entity.User;
import com.example.carrental.enums.UserRole;
import com.example.carrental.exception.base.EntityNotFoundException;
import com.example.carrental.exception.user.UserAlreadyExistsException;
import com.example.carrental.mapper.user.UserMapper;
import com.example.carrental.repository.UserRepository;
import com.example.carrental.security.jwt.JwtUtil;
import com.example.carrental.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Override
    public UserResponseDto register(UserRegistrationRequestDto request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(UserRole.CUSTOMER);
        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Override
    public JwtAuthenticationDto login(UserLoginRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        String email = request.getEmail();

        String accessToken = jwtUtil.generateAccessToken(email);
        String refreshToken = jwtUtil.generateRefreshToken(email);

        return new JwtAuthenticationDto(accessToken, refreshToken);
    }

    @Override
    public JwtAuthenticationDto refreshToken(RefreshTokenDto request) {
        String requestToken = request.getRefreshToken();
        String email = jwtUtil.getEmailFromJwtToken(requestToken);

        if (!jwtUtil.validateJwtToken(requestToken)) {
            throw new BadCredentialsException("Refresh token is invalid or expired");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        return new JwtAuthenticationDto(newAccessToken, newRefreshToken);
    }
}
