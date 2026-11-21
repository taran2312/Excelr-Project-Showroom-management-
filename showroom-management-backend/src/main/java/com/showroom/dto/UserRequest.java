package com.showroom.dto;

import com.showroom.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Admin create/update of a staff account. Password is required on create, optional on update. */
@Data
public class UserRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    @Size(max = 20)
    private String phone;

    @NotNull(message = "Role is required")
    private Role role;

    private Long showroomId;

    private Boolean active;
}
