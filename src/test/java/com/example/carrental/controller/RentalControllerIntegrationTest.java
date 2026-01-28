package com.example.carrental.controller;

import com.example.carrental.dto.exception.ErrorResponse;
import com.example.carrental.dto.jwt.JwtAuthenticationDto;
import com.example.carrental.dto.rental.RentalRequestDto;
import com.example.carrental.dto.rental.RentalResponseDto;
import com.example.carrental.dto.rental.RentalUpdateRequestDto;
import com.example.carrental.dto.user.UserLoginRequestDto;
import com.example.carrental.entity.Car;
import com.example.carrental.entity.Rental;
import com.example.carrental.entity.User;
import com.example.carrental.enums.CarStatus;
import com.example.carrental.enums.CarType;
import com.example.carrental.enums.UserRole;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.RentalRepository;
import com.example.carrental.repository.UserRepository;
import com.example.carrental.util.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RentalControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private CarRepository carRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RentalRepository rentalRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User defaultCustomer;
    private Car defaultCar;
    private String defaultToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE cars CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE rentals CASCADE");

        defaultCustomer = createTestUser("customer@email.com", UserRole.CUSTOMER);
        defaultCar = createTestCar("AA0000AA", CarStatus.AVAILABLE);
        defaultToken = loginAndGetToken("customer@email.com");
    }

    @Nested
    @DisplayName("POST /api/rentals (Create Rental)")
    class CreateRental {

        @Test
        @DisplayName("Success: Should create rental and change car status to RENTED")
        void shouldCreateRentalSuccess() {
            RentalRequestDto requestDto = createRentalRequest(
                    defaultCar.getId(),
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(5)
            );

            ResponseEntity<RentalResponseDto> response = executeRequest(
                    createUrl("/api/rentals"),
                    HttpMethod.POST,
                    requestDto,
                    defaultToken,
                    RentalResponseDto.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(defaultCar.getId(), response.getBody().getCarId());

            Car updatedCar = carRepository.findById(defaultCar.getId()).orElseThrow();
            assertEquals(CarStatus.RENTED, updatedCar.getStatus());
        }

        @Test
        @DisplayName("Fail: Should return 409 if Car is not AVAILABLE")
        void shouldFailWhenCarNotAvailable() {
            defaultCar.setStatus(CarStatus.RENTED);
            carRepository.save(defaultCar);

            RentalRequestDto requestDto = createRentalRequest(
                    defaultCar.getId(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(5)
            );

            ResponseEntity<ErrorResponse> response = executeRequest(
                    createUrl("/api/rentals"),
                    HttpMethod.POST,
                    requestDto,
                    defaultToken,
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());

            assertTrue(response.getBody().message().contains("Car is not available"));
        }

        @Test
        @DisplayName("Fail: Should return 400 if Dates Logic Invalid (Validator)")
        void shouldFailWhenDatesInvalid() {
            RentalRequestDto requestDto = createRentalRequest(
                    defaultCar.getId(),
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(2)
            );

            ResponseEntity<ErrorResponse> response = executeRequest(
                    createUrl("/api/rentals"),
                    HttpMethod.POST,
                    requestDto,
                    defaultToken,
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());

            assertEquals("Validation failed", response.getBody().message());

            Map<String, String> errors = response.getBody().details();
            assertNotNull(errors);

            boolean messageFound = errors.values().stream()
                    .anyMatch(msg -> msg.contains("Return date") || msg.contains("after rental date"));

            assertTrue(messageFound);
        }

        @Test
        @DisplayName("Fail: Should return 404 if Car does not exist")
        void shouldFailWhenCarNotFound() {
            RentalRequestDto requestDto = createRentalRequest(
                    9999L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)
            );

            ResponseEntity<ErrorResponse> response = executeRequest(
                    createUrl("/api/rentals"),
                    HttpMethod.POST,
                    requestDto,
                    defaultToken,
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().message().contains("Car not found"));
        }

        @Test
        @DisplayName("Fail: Should return 401 if User is not authorized")
        void shouldFailWhenNoToken() {
            RentalRequestDto requestDto = createRentalRequest(
                    defaultCar.getId(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)
            );

            HttpEntity<RentalRequestDto> request = new HttpEntity<>(requestDto);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    createUrl("/api/rentals"),
                    request,
                    String.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("GET /api/rentals (Read Operations)")
    class GetRentals {

        @Test
        @DisplayName("GET /my: Should return only current user's rentals")
        void shouldReturnMyRentals() {
            User otherUser = createTestUser("other@email.com", UserRole.CUSTOMER);
            Car otherCar = createTestCar("BB1111BB", CarStatus.RENTED);
            saveRental(otherUser, otherCar, false);

            saveRental(defaultCustomer, defaultCar, false);

            Car car2 = createTestCar("CC2222CC", CarStatus.AVAILABLE);
            saveRental(defaultCustomer, car2, true);

            ResponseEntity<RentalResponseDto[]> response = executeRequest(
                    createUrl("/api/rentals/my"),
                    HttpMethod.GET,
                    null,
                    defaultToken,
                    RentalResponseDto[].class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(2, response.getBody().length);
            assertEquals(defaultCustomer.getId(), response.getBody()[0].getUserId());
        }

        @Test
        @DisplayName("GET /active: Manager should see ALL active rentals")
        void shouldReturnActiveRentalsForManager() {
            User manager = createTestUser("manager@email.com", UserRole.MANAGER);
            String managerToken = loginAndGetToken("manager@email.com");

            saveRental(defaultCustomer, defaultCar, false);
            Car car2 = createTestCar("CC3333CC", CarStatus.AVAILABLE);
            saveRental(defaultCustomer, car2, true);

            ResponseEntity<RentalResponseDto[]> response = executeRequest(
                    createUrl("/api/rentals/active"),
                    HttpMethod.GET,
                    null,
                    managerToken,
                    RentalResponseDto[].class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().length);
            assertEquals(defaultCar.getId(), response.getBody()[0].getCarId());
        }

        @Test
        @DisplayName("GET /active: Customer cannot access (Forbidden)")
        void shouldFailActiveForCustomer() {
            ResponseEntity<String> response = executeRequest(
                    createUrl("/api/rentals/active"),
                    HttpMethod.GET,
                    null,
                    defaultToken,
                    String.class
            );
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("POST /api/rentals/{id}/return (Return Car)")
    class ReturnRental {

        @Test
        @DisplayName("Success: Manager should return car")
        void shouldReturnCarSuccess() {
            User manager = createTestUser("manager@email.com", UserRole.MANAGER);
            String managerToken = loginAndGetToken("manager@email.com");
            Rental rental = saveRental(defaultCustomer, defaultCar, false);

            ResponseEntity<RentalResponseDto> response = executeRequest(
                    createUrl("/api/rentals/" + rental.getId() + "/return"),
                    HttpMethod.POST,
                    null,
                    managerToken,
                    RentalResponseDto.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getActualReturnDate());
            Car updatedCar = carRepository.findById(defaultCar.getId()).orElseThrow();
            assertEquals(CarStatus.AVAILABLE, updatedCar.getStatus());
        }

        @Test
        @DisplayName("Fail: Cannot return already returned car")
        void shouldFailWhenAlreadyReturned() {
            User manager = createTestUser("manager@email.com", UserRole.MANAGER);
            String managerToken = loginAndGetToken("manager@email.com");
            Rental rental = saveRental(defaultCustomer, defaultCar, true);

            ResponseEntity<ErrorResponse> response = executeRequest(
                    createUrl("/api/rentals/" + rental.getId() + "/return"),
                    HttpMethod.POST,
                    null,
                    managerToken,
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().message().contains("already finished") || response.getBody().message().contains("Rental"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/rentals/{id}/update (Update Rental)")
    class UpdateRental {

        @Test
        @DisplayName("Success: Manager can extend rental")
        void shouldUpdateRentalSuccess() {
            User manager = createTestUser("manager2@email.com", UserRole.MANAGER);
            String managerToken = loginAndGetToken("manager2@email.com");
            Rental rental = saveRental(defaultCustomer, defaultCar, false);

            RentalUpdateRequestDto updateDto = new RentalUpdateRequestDto();
            updateDto.setReturnDate(rental.getReturnDate().plusDays(5));

            ResponseEntity<RentalResponseDto> response = executeRequest(
                    createUrl("/api/rentals/" + rental.getId() + "/update"),
                    HttpMethod.PATCH,
                    updateDto,
                    managerToken,
                    RentalResponseDto.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(updateDto.getReturnDate(), response.getBody().getReturnDate());
        }

        @Test
        @DisplayName("Fail: Invalid dates (@Future validation)")
        void shouldFailWhenInvalidDates() {
            User manager = createTestUser("manager3@email.com", UserRole.MANAGER);
            String managerToken = loginAndGetToken("manager3@email.com");
            Rental rental = saveRental(defaultCustomer, defaultCar, false);

            RentalUpdateRequestDto updateDto = new RentalUpdateRequestDto();
            updateDto.setReturnDate(LocalDate.now().minusDays(10));

            ResponseEntity<ErrorResponse> response = executeRequest(
                    createUrl("/api/rentals/" + rental.getId() + "/update"),
                    HttpMethod.PATCH,
                    updateDto,
                    managerToken,
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Validation failed", response.getBody().message());

            Map<String, String> errors = response.getBody().details();
            assertNotNull(errors);
            assertTrue(errors.containsKey("returnDate"));
        }
    }

    private Rental saveRental(User user, Car car, boolean isReturned) {
        Rental rental = new Rental();
        rental.setUser(user);
        rental.setCar(car);
        rental.setRentalDate(LocalDate.now().minusDays(2));
        rental.setReturnDate(LocalDate.now().plusDays(2));

        if (isReturned) {
            rental.setActualReturnDate(LocalDate.now());
            car.setStatus(CarStatus.AVAILABLE);
        } else {
            rental.setActualReturnDate(null);
            car.setStatus(CarStatus.RENTED);
        }
        carRepository.save(car);
        return rentalRepository.save(rental);
    }

    private <T, D> ResponseEntity<T> executeRequest(
            String url,
            HttpMethod httpMethod,
            D dto,
            String token,
            Class<T> responseType
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<D> request = new HttpEntity<>(dto, headers);

        return restTemplate.exchange(url, httpMethod, request, responseType);
    }

    private RentalRequestDto createRentalRequest(Long carId, LocalDate start, LocalDate end) {
        RentalRequestDto dto = new RentalRequestDto();
        dto.setCarId(carId);
        dto.setRentalDate(start);
        dto.setReturnDate(end);
        return dto;
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

    private Car createTestCar(String plate, CarStatus status) {
        Car car = new Car();
        car.setBrand("Toyota");
        car.setModel("Camry");
        car.setType(CarType.SEDAN);
        car.setDailyFee(BigDecimal.TEN);
        car.setLicensePlate(plate);
        car.setColor("Black");
        car.setStatus(status);
        car.setDeleted(false);
        return carRepository.save(car);
    }

    private String loginAndGetToken(String email) {
        UserLoginRequestDto loginRequest = new UserLoginRequestDto();
        loginRequest.setEmail(email);
        loginRequest.setPassword("password");

        ResponseEntity<JwtAuthenticationDto> response = restTemplate.postForEntity(
                createUrl("/api/auth/login"),
                loginRequest,
                JwtAuthenticationDto.class
        );
        assertNotNull(response.getBody());
        return response.getBody().getToken();
    }
}