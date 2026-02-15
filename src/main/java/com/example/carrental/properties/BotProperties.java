package com.example.carrental.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "bot")
@Data
public class BotProperties {
    private String username;
    private String token;
    private Long adminChatId;
}

