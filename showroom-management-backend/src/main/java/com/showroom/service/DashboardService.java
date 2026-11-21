package com.showroom.service;

import com.showroom.dto.DashboardStats;
import com.showroom.entity.SaleStatus;
import com.showroom.entity.TestDriveStatus;
import com.showroom.entity.VehicleStatus;
import com.showroom.mapper.EntityMapper;
import com.showroom.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    private final ShowroomRepository showroomRepository;
    private final SaleRepository saleRepository;
    private final TestDriveRepository testDriveRepository;

    public DashboardStats getStats() {
        return DashboardStats.builder()
                .totalVehicles(vehicleRepository.count())
                .availableVehicles(vehicleRepository.countByStatus(VehicleStatus.AVAILABLE))
                .reservedVehicles(vehicleRepository.countByStatus(VehicleStatus.RESERVED))
                .soldVehicles(vehicleRepository.countByStatus(VehicleStatus.SOLD))
                .totalCustomers(customerRepository.count())
                .totalShowrooms(showroomRepository.count())
                .pendingSales(saleRepository.countByStatus(SaleStatus.PENDING))
                .completedSales(saleRepository.countByStatus(SaleStatus.COMPLETED))
                .totalRevenue(saleRepository.totalCompletedRevenue())
                .upcomingTestDrives(testDriveRepository.countByStatusAndScheduledAtAfter(
                        TestDriveStatus.SCHEDULED, LocalDateTime.now()))
                .recentSales(saleRepository.findTop5ByOrderByCreatedAtDesc().stream()
                        .map(EntityMapper::toDto).toList())
                .build();
    }
}
