package com.example.carrental.dto.user;

import com.example.carrental.validation.annotation.Password;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegistrationRequestDto {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    @Password
    private String password;

    @NotBlank
    @Size(min = 8, max = 100)
    @Password
    private String confirmPassword;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

}
