package com.showroom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Code is required")
    @Size(max = 20)
    private String code;

    @Size(max = 255)
    private String address;

    @Size(max = 80)
    private String city;

    @Size(max = 20)
    private String phone;

    @Email(message = "Email must be valid")
    private String email;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private long vehicleCount;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;
}
