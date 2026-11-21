package com.showroom.dto;

import com.showroom.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Outbound view of a user (never includes the password hash). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private Long showroomId;
    private String showroomName;
    private boolean active;
    private LocalDateTime createdAt;
}
