package com.example.carrental.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "file")
@Data
public class FileProperties {
    private String baseDir;
    private String imagesUploadDir;
    private Subdirs subdirs;

    @Data
    public static class Subdirs {
        private String car;
    }
}
