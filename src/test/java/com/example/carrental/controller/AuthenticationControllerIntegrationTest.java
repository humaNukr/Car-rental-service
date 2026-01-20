package com.example.carrental.controller;

import com.example.carrental.dto.jwt.JwtAuthenticationDto;
import com.example.carrental.dto.user.UserLoginRequestDto;
import com.example.carrental.dto.user.UserRegistrationRequestDto;
import com.example.carrental.dto.user.UserResponseDto;
import com.example.carrental.repository.UserRepository;
import com.example.carrental.util.BaseIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;


import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationControllerIntegrationTest extends BaseIT {
    @Autowired
    private TestRestTemplate testRestTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }

    @Test
    @DisplayName("Full Cycle: Register and Login successfully")
    void registerAndLogin_Success(){
        UserRegistrationRequestDto registerRequest = new UserRegistrationRequestDto();
        registerRequest.setEmail("driver@test.com");
        registerRequest.setPassword("SuperPass123!");
        registerRequest.setConfirmPassword("SuperPass123!");
        registerRequest.setFirstName("Max");
        registerRequest.setLastName("Verstappen");

        ResponseEntity<UserResponseDto> responseDto = testRestTemplate.postForEntity(
                String.format("http://localhost:%d/api/auth/register", port),
                registerRequest,
                UserResponseDto.class);

        assertEquals(HttpStatus.OK, responseDto.getStatusCode());
        assertNotNull(responseDto.getBody());
        assertEquals(registerRequest.getEmail(), responseDto.getBody().getEmail());
        assertNotNull(responseDto.getBody().getId());

        assertTrue(userRepository.findByEmail("driver@test.com").isPresent());

        UserLoginRequestDto loginRequest = new UserLoginRequestDto();
        loginRequest.setEmail("driver@test.com");
        loginRequest.setPassword("SuperPass123!");

        ResponseEntity<JwtAuthenticationDto> authDto = testRestTemplate.postForEntity(
                String.format("http://localhost:%d/api/auth/login", port),
                loginRequest,
                JwtAuthenticationDto.class);
        assertEquals(HttpStatus.OK, authDto.getStatusCode());
        assertNotNull(authDto.getBody());
        assertNotNull(authDto.getBody().getToken());
        assertNotNull(authDto.getBody().getRefreshToken());
    }

    @Test
    @DisplayName("Login should fail with wrong password")
    void loginShouldFailWithWrongPassword(){
        UserRegistrationRequestDto registerRequest = new UserRegistrationRequestDto();
        registerRequest.setEmail("driver@test.com");
        registerRequest.setPassword("SuperPass123!");
        registerRequest.setConfirmPassword("SuperPass123!");
        registerRequest.setFirstName("Max");
        registerRequest.setLastName("Verstappen");

        testRestTemplate.postForEntity(
                String.format("http://localhost:%d/api/auth/register", port),
                registerRequest,
                UserResponseDto.class);

        UserLoginRequestDto wrongLogin = new UserLoginRequestDto();
        wrongLogin.setEmail("driver@test.com");
        wrongLogin.setPassword("WrongPass000!");

        ResponseEntity<JwtAuthenticationDto> authDto = testRestTemplate.postForEntity(
                String.format("http://localhost:%d/api/auth/login", port),
                wrongLogin,
                JwtAuthenticationDto.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, authDto.getStatusCode());
    }
}
