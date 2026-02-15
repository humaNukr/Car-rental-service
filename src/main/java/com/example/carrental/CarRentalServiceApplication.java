package com.example.carrental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class CarRentalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarRentalServiceApplication.class, args);
    }

}
