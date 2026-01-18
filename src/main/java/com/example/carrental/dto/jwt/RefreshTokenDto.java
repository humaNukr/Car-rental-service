package com.example.carrental.dto.jwt;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenDto {
    @NotBlank
    private String refreshToken;

}
