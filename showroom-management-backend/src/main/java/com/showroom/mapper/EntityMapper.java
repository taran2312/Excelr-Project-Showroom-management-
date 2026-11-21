package com.showroom.mapper;

import com.showroom.dto.*;
import com.showroom.entity.*;

import java.math.BigDecimal;

/** Entity -> DTO conversions. Call inside a transaction so lazy associations can load. */
public final class EntityMapper {

    private EntityMapper() {
    }

    public static UserDto toDto(User u) {
        return UserDto.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole())
                .showroomId(u.getShowroom() != null ? u.getShowroom().getId() : null)
                .showroomName(u.getShowroom() != null ? u.getShowroom().getName() : null)
                .active(u.isActive())
                .createdAt(u.getCreatedAt())
                .build();
    }

    public static ShowroomDto toDto(Showroom s, long vehicleCount) {
        return ShowroomDto.builder()
                .id(s.getId())
                .name(s.getName())
                .code(s.getCode())
                .address(s.getAddress())
                .city(s.getCity())
                .phone(s.getPhone())
                .email(s.getEmail())
                .vehicleCount(vehicleCount)
                .createdAt(s.getCreatedAt())
                .build();
    }

    public static VehicleDto toDto(Vehicle v) {
        return VehicleDto.builder()
                .id(v.getId())
                .vin(v.getVin())
                .make(v.getMake())
                .model(v.getModel())
                .variant(v.getVariant())
                .year(v.getYear())
                .color(v.getColor())
                .fuelType(v.getFuelType())
                .transmission(v.getTransmission())
                .price(v.getPrice())
                .mileage(v.getMileage())
                .status(v.getStatus())
                .showroomId(v.getShowroom().getId())
                .showroomName(v.getShowroom().getName())
                .description(v.getDescription())
                .createdAt(v.getCreatedAt())
                .build();
    }

    public static CustomerDto toDto(Customer c) {
        return CustomerDto.builder()
                .id(c.getId())
                .fullName(c.getFullName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .address(c.getAddress())
                .city(c.getCity())
                .notes(c.getNotes())
                .createdAt(c.getCreatedAt())
                .build();
    }

    public static SaleDto toDto(Sale s) {
        BigDecimal discount = s.getDiscount() != null ? s.getDiscount() : BigDecimal.ZERO;
        return SaleDto.builder()
                .id(s.getId())
                .invoiceNumber(s.getInvoiceNumber())
                .vehicleId(s.getVehicle().getId())
                .vehicleLabel(vehicleLabel(s.getVehicle()))
                .customerId(s.getCustomer().getId())
                .customerName(s.getCustomer().getFullName())
                .salespersonId(s.getSalesperson().getId())
                .salespersonName(s.getSalesperson().getFullName())
                .salePrice(s.getSalePrice())
                .discount(discount)
                .finalAmount(s.getSalePrice().subtract(discount))
                .paymentMethod(s.getPaymentMethod())
                .status(s.getStatus())
                .saleDate(s.getSaleDate())
                .notes(s.getNotes())
                .createdAt(s.getCreatedAt())
                .build();
    }

    public static TestDriveDto toDto(TestDrive t) {
        return TestDriveDto.builder()
                .id(t.getId())
                .vehicleId(t.getVehicle().getId())
                .vehicleLabel(vehicleLabel(t.getVehicle()))
                .customerId(t.getCustomer().getId())
                .customerName(t.getCustomer().getFullName())
                .salespersonId(t.getSalesperson() != null ? t.getSalesperson().getId() : null)
                .salespersonName(t.getSalesperson() != null ? t.getSalesperson().getFullName() : null)
                .scheduledAt(t.getScheduledAt())
                .status(t.getStatus())
                .feedback(t.getFeedback())
                .createdAt(t.getCreatedAt())
                .build();
    }

    public static String vehicleLabel(Vehicle v) {
        return v.getYear() + " " + v.getMake() + " " + v.getModel()
                + (v.getVariant() != null && !v.getVariant().isBlank() ? " " + v.getVariant() : "")
                + " (" + v.getVin() + ")";
    }

    /** Trims a string and turns blanks into null. */
    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
