package com.showroom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.showroom.entity.VehicleStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank(message = "VIN is required")
    @Size(min = 5, max = 17, message = "VIN must be 5-17 characters")
    private String vin;

    @NotBlank(message = "Make is required")
    @Size(max = 50)
    private String make;

    @NotBlank(message = "Model is required")
    @Size(max = 50)
    private String model;

    @Size(max = 50)
    private String variant;

    @NotNull(message = "Year is required")
    @Min(value = 1950, message = "Year must be 1950 or later")
    @Max(value = 2100, message = "Year is too far in the future")
    private Integer year;

    @Size(max = 30)
    private String color;

    @Size(max = 20)
    private String fuelType;

    @Size(max = 20)
    private String transmission;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    private BigDecimal price;

    @Min(value = 0, message = "Mileage cannot be negative")
    private Integer mileage;

    /** Optional on create (defaults to AVAILABLE). RESERVED/SOLD are normally driven by sales. */
    private VehicleStatus status;

    @NotNull(message = "Showroom is required")
    private Long showroomId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String showroomName;

    @Size(max = 1000)
    private String description;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;
}
