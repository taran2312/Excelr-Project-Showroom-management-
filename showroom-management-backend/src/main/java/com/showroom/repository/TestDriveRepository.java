package com.showroom.repository;

import com.showroom.entity.TestDrive;
import com.showroom.entity.TestDriveStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TestDriveRepository extends JpaRepository<TestDrive, Long> {

    @EntityGraph(attributePaths = {"vehicle", "customer", "salesperson"})
    List<TestDrive> findAllByOrderByScheduledAtDesc();

    @EntityGraph(attributePaths = {"vehicle", "customer", "salesperson"})
    List<TestDrive> findByStatusOrderByScheduledAtDesc(TestDriveStatus status);

    long countByStatusAndScheduledAtAfter(TestDriveStatus status, LocalDateTime after);

    boolean existsByVehicleId(Long vehicleId);

    boolean existsByCustomerId(Long customerId);

    boolean existsBySalespersonId(Long userId);
}
