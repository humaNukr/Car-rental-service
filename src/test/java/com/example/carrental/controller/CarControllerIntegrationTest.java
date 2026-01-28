package com.example.carrental.controller;

import com.example.carrental.dto.car.CarResponseDto;
import com.example.carrental.entity.Car;
import com.example.carrental.enums.CarStatus;
import com.example.carrental.enums.CarType;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.util.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarControllerIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE cars CASCADE");
        createTestCars();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE cars CASCADE");
    }

    @Nested
    @DisplayName("Filter tests")
    class FilterTests {
        @Test
        @DisplayName("filter by brand Should return only BMWs")
        void givenCarsOfDifferentBrandsWhenFilterByBMWThenOnlyBMWsAreReturned() {
            String url = createUrl("/api/cars?brands=BMW");

            ResponseEntity<List<CarResponseDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            List<CarResponseDto> cars = response.getBody();

            assertNotNull(cars);
            assertEquals(2, cars.size());
            assertTrue(cars.stream().allMatch(c -> c.getBrand().equals("BMW")));
        }

        @Test
        @DisplayName("filter by type: Should return SUV and SEDAN")
        void givenCarsOfDifferentTypesWhenFilterBySuvAndSedanThenOnlySuvsAndSedansAreReturned() {
            String url = createUrl("/api/cars?types=SUV,SEDAN");

            ResponseEntity<List<CarResponseDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            List<CarResponseDto> cars = response.getBody();

            assertNotNull(cars);
            assertEquals(3, cars.size());
        }

        @Test
        @DisplayName("filter by fee range: Should return cars between 40 and 100")
        void givenCarsOfDifferentFeeRangesWhenFilterByFeeRangeThenOnlyCarWithFeesInThisRangeAreReturned() {
            String url = createUrl("/api/cars?minDailyFee=40&maxDailyFee=100");

            ResponseEntity<List<CarResponseDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            List<CarResponseDto> cars = response.getBody();
            assertNotNull(cars);

            assertEquals(3, cars.size());
            assertTrue(cars.stream().allMatch(c ->
                    c.getDailyFee().compareTo(BigDecimal.valueOf(40)) >= 0 &&
                            c.getDailyFee().compareTo(BigDecimal.valueOf(100)) <= 0
            ));
        }

        @Test
        @DisplayName("complex filter: Black BMW")
        void givenCarsWhenFilterByBlackBMWThenReturnOnlyBlackBMWs() {
            String url = createUrl("/api/cars?brands=BMW&colors=Black");

            ResponseEntity<List<CarResponseDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            List<CarResponseDto> cars = response.getBody();

            assertNotNull(cars);
            assertEquals(1, cars.size());
            assertEquals("X5", cars.getFirst().getModel());
        }
    }

    private void createTestCars() {
        saveCar("BMW", "X5", CarType.SUV, "Black",
                "AA0001AA", 150, CarStatus.AVAILABLE);

        saveCar("Toyota", "Camry", CarType.SEDAN, "Purple",
                "AA0002AA", 50, CarStatus.AVAILABLE);

        saveCar("BMW", "320", CarType.SEDAN, "White",
                "AA0003AA", 90, CarStatus.AVAILABLE);

        saveCar("Audi", "A4", CarType.WAGON, "Blue",
                "AA0004AA", 80, CarStatus.AVAILABLE);
    }

    private void saveCar(String brand, String model, CarType type, String color,
                         String plate, double fee, CarStatus status) {
        Car car = new Car();
        car.setBrand(brand);
        car.setModel(model);
        car.setType(type);
        car.setColor(color);
        car.setLicensePlate(plate);
        car.setDailyFee(BigDecimal.valueOf(fee));
        car.setStatus(status);
        car.setDeleted(false);
        carRepository.save(car);
    }

}