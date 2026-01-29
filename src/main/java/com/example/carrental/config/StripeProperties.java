package com.example.carrental.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "stripe")
@Data
public class StripeProperties {
    private String apiKey;
    private String successUrl;
    private String cancelUrl;
}
