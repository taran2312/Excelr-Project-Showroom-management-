package com.showroom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.showroom.entity.PaymentMethod;
import com.showroom.entity.SaleStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String invoiceNumber;

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

    @NotNull(message = "Sale price is required")
    @DecimalMin(value = "0.0", message = "Sale price cannot be negative")
    private BigDecimal salePrice;

    @DecimalMin(value = "0.0", message = "Discount cannot be negative")
    private BigDecimal discount;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal finalAmount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private SaleStatus status;

    /** Optional: defaults to today. */
    private LocalDate saleDate;

    @Size(max = 1000)
    private String notes;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;
}
