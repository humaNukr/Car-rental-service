package com.example.carrental.controller;

import com.example.carrental.dto.jwt.JwtAuthenticationDto;
import com.example.carrental.dto.user.UserLoginRequestDto;
import com.example.carrental.dto.user.UserResponseDto;
import com.example.carrental.dto.user.UserRoleUpdateDto;
import com.example.carrental.dto.user.UserUpdateRequestDto;
import com.example.carrental.entity.User;
import com.example.carrental.enums.UserRole;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class UserControllerIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }

    @Nested
    @DisplayName("Get My Profile")
    class GetMyProfile {
        @Test
        @DisplayName("Should return current user info")
        void shouldReturnCurrentUser() {
            String email = "me@test.com";
            createTestUser(email, UserRole.CUSTOMER);
            String token = loginAndGetToken(email);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<UserResponseDto> response = testRestTemplate.exchange(
                    createUrl("/api/users/me"),
                    HttpMethod.GET,
                    request,
                    UserResponseDto.class
            );


            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(email, response.getBody().getEmail());
        }
    }

    @Nested
    @DisplayName("Update My Profile")
    class UpdateMyProfile {
        @Test
        @DisplayName("Should update lastname and firstname")
        void shouldUpdateLastNameAndFirstName() {
            String email = "me@test.com";
            createTestUser(email, UserRole.CUSTOMER);
            String token = loginAndGetToken(email);

            var requestDto = new UserUpdateRequestDto();
            requestDto.setFirstName("John");
            requestDto.setLastName("Doe");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<UserUpdateRequestDto> request = new HttpEntity<>(requestDto, headers);

            ResponseEntity<UserResponseDto> response = testRestTemplate.exchange(
                    createUrl("/api/users/me"),
                    HttpMethod.PATCH,
                    request,
                    UserResponseDto.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(email, response.getBody().getEmail());
            assertEquals("John", response.getBody().getFirstName());
            assertEquals("Doe", response.getBody().getLastName());
        }
    }

    @Nested
    @DisplayName("Update User's Role")
    class UpdateUserRole {
        @Test
        @DisplayName("Should update user's role when role is ADMIN")
        void shouldUpdateUserRole() {
            createTestUser("admin@email.com", UserRole.ADMIN);
            User targetUser = createTestUser("target@gmail.com", UserRole.CUSTOMER);

            String adminToken = loginAndGetToken("admin@email.com");

            var dto = new UserRoleUpdateDto();
            dto.setRole(UserRole.MANAGER);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<UserRoleUpdateDto> request = new HttpEntity<>(dto, headers);

            ResponseEntity<UserResponseDto> response = testRestTemplate.exchange(
                    createUrl("/api/users/" + targetUser.getId() + "/role"),
                    HttpMethod.PATCH,
                    request,
                    UserResponseDto.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(UserRole.MANAGER.name(), response.getBody().getRole());
            assertEquals("target@gmail.com", response.getBody().getEmail());

            User updatedUser = userRepository.findById(targetUser.getId()).get();
            assertEquals(UserRole.MANAGER, updatedUser.getRole());
        }

        @Test
        @DisplayName("Should forbid update user's role when role is not ADMIN")
        void shouldForbidUpdateUserRole() {
            createTestUser("customer@email.com", UserRole.CUSTOMER);
            User targetUser = createTestUser("target@gmail.com", UserRole.CUSTOMER);

            String customerToken = loginAndGetToken("customer@email.com");

            var dto = new UserRoleUpdateDto();
            dto.setRole(UserRole.MANAGER);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(customerToken);
            HttpEntity<UserRoleUpdateDto> request = new HttpEntity<>(dto, headers);

            ResponseEntity<UserResponseDto> response = testRestTemplate.exchange(
                    createUrl("/api/users/" + targetUser.getId() + "/role"),
                    HttpMethod.PATCH,
                    request,
                    UserResponseDto.class
            );
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

            User user = userRepository.findById(targetUser.getId()).get();
            assertEquals(UserRole.CUSTOMER, user.getRole());
        }
    }

    private User createTestUser(String email, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(role);
        return userRepository.save(user);
    }

    private String loginAndGetToken(String email) {
        UserLoginRequestDto loginRequest = new UserLoginRequestDto();
        loginRequest.setEmail(email);
        loginRequest.setPassword("password");

        ResponseEntity<JwtAuthenticationDto> response = testRestTemplate.postForEntity(
                createUrl("/api/auth/login"),
                loginRequest,
                JwtAuthenticationDto.class
        );
        return response.getBody().getToken();
    }

    private String createUrl(String uri) {
        return "http://localhost:" + port + uri;
    }

}