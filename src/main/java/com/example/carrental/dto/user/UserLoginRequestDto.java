package com.example.carrental.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

@Data
public class UserLoginRequestDto {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @ToString.Exclude
    private String password;
}
