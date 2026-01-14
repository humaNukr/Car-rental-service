package com.example.carrental.dto.user;

import lombok.Data;

@Data
public class UserResponseDto {
    private Long id;
    private String email;
    private String lastName;
    private String firstName;
    private String role;
}
