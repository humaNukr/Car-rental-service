package com.example.carrental.controller;

import com.example.carrental.dto.jwt.JwtAuthenticationDto;
import com.example.carrental.dto.jwt.RefreshTokenDto;
import com.example.carrental.dto.user.UserLoginRequestDto;
import com.example.carrental.dto.user.UserRegistrationRequestDto;
import com.example.carrental.dto.user.UserResponseDto;
import com.example.carrental.repository.UserRepository;
import com.example.carrental.util.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthenticationControllerIntegrationTest extends BaseIntegrationTest {
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

    @Nested
    @DisplayName("Registration & Login")
    class RegistrationAndLoginTests {

        @Test
        @DisplayName("Full Cycle: Register and Login successfully")
        void registerAndLoginSuccess() {
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
        void loginShouldFailWithWrongPassword() {
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

            assertEquals(HttpStatus.UNAUTHORIZED, authDto.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Refresh Token")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void refreshTokenSuccess() {
            UserRegistrationRequestDto registerRequest = new UserRegistrationRequestDto();
            registerRequest.setEmail("refresh@test.com");
            registerRequest.setPassword("Pass123!");
            registerRequest.setConfirmPassword("Pass123!");
            registerRequest.setFirstName("John");
            registerRequest.setLastName("Doe");

            testRestTemplate.postForEntity(
                    String.format("http://localhost:%d/api/auth/register", port),
                    registerRequest,
                    UserResponseDto.class);

            UserLoginRequestDto loginRequest = new UserLoginRequestDto();
            loginRequest.setEmail("refresh@test.com");
            loginRequest.setPassword("Pass123!");

            ResponseEntity<JwtAuthenticationDto> loginResponse = testRestTemplate.postForEntity(
                    String.format("http://localhost:%d/api/auth/login", port),
                    loginRequest,
                    JwtAuthenticationDto.class);

            assertNotNull(loginResponse.getBody());
            String oldRefreshToken = loginResponse.getBody().getRefreshToken();
            assertNotNull(oldRefreshToken);

            RefreshTokenDto refreshRequest = new RefreshTokenDto();
            refreshRequest.setRefreshToken(oldRefreshToken);

            ResponseEntity<JwtAuthenticationDto> refreshResponse = testRestTemplate.postForEntity(
                    String.format("http://localhost:%d/api/auth/refresh", port),
                    refreshRequest,
                    JwtAuthenticationDto.class);

            assertEquals(HttpStatus.OK, refreshResponse.getStatusCode());
            assertNotNull(refreshResponse.getBody());

            assertNotNull(refreshResponse.getBody().getToken());
            assertNotNull(refreshResponse.getBody().getRefreshToken());

            assertNotEquals(refreshResponse.getBody().getToken(), oldRefreshToken);
        }

        @Test
        @DisplayName("Refresh should fail with bad token")
        void refresh_Fail_BadToken() {
            RefreshTokenDto badRequest = new RefreshTokenDto();
            badRequest.setRefreshToken("some-fake-jwt-token-string");

            ResponseEntity<Object> response = testRestTemplate.postForEntity(
                    String.format("http://localhost:%d/api/auth/refresh", port),
                    badRequest,
                    Object.class);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }
}
