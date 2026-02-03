package com.example.carrental.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rental")
@Data
public class RentalProperties {
    private Fine fine = new  Fine();

    @Data
    public static class Fine {
        private Double lateReturnMultiplier = 1.0;
    }
}
