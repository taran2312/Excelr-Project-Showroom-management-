package com.showroom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.showroom.entity.TestDriveStatus;
import jakarta.validation.constraints.NotNull;
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
public class TestDriveDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull(message = "Vehicle is required")
    private Long vehicleId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String vehicleLabel;

    @NotNull(message = "Customer is required")
    private Long customerId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String customerName;

    /** Optional: defaults to the logged-in user. */
    private Long salespersonId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String salespersonName;

    @NotNull(message = "Date/time is required")
    private LocalDateTime scheduledAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private TestDriveStatus status;

    @Size(max = 1000)
    private String feedback;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;
}
