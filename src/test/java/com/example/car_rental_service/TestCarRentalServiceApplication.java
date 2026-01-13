package com.example.car_rental_service;

import org.springframework.boot.SpringApplication;

public class TestCarRentalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(CarRentalServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
