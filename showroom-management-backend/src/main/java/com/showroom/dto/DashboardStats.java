package com.showroom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private long totalVehicles;
    private long availableVehicles;
    private long reservedVehicles;
    private long soldVehicles;
    private long totalCustomers;
    private long totalShowrooms;
    private long pendingSales;
    private long completedSales;
    private BigDecimal totalRevenue;
    private long upcomingTestDrives;
    private List<SaleDto> recentSales;
}
