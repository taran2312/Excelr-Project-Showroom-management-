package com.showroom.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 5, max = 17)
    @Column(nullable = false, unique = true, length = 17)
    private String vin;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String make;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String model;

    @Size(max = 50)
    @Column(length = 50)
    private String variant;

    @NotNull
    @Min(1950)
    @Max(2100)
    @Column(name = "model_year", nullable = false)
    private Integer year;

    @Size(max = 30)
    @Column(length = 30)
    private String color;

    @Size(max = 20)
    @Column(length = 20)
    private String fuelType;

    @Size(max = 20)
    @Column(length = 20)
    private String transmission;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Min(0)
    private Integer mileage;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showroom_id", nullable = false)
    private Showroom showroom;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
