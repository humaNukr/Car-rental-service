package com.example.carrental.dto.user;

import com.example.carrental.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRoleUpdateDto {
    @NotNull
    private UserRole role;
}
