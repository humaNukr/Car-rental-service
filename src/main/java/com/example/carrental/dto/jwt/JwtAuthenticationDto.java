package com.example.carrental.dto.jwt;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtAuthenticationDto {
    private String token;
    private String refreshToken;
}
