package com.showroom.repository;

import com.showroom.entity.Sale;
import com.showroom.entity.SaleStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    @EntityGraph(attributePaths = {"vehicle", "customer", "salesperson"})
    List<Sale> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"vehicle", "customer", "salesperson"})
    List<Sale> findByStatusOrderByCreatedAtDesc(SaleStatus status);

    @EntityGraph(attributePaths = {"vehicle", "customer", "salesperson"})
    List<Sale> findTop5ByOrderByCreatedAtDesc();

    boolean existsByVehicleIdAndStatusIn(Long vehicleId, Collection<SaleStatus> statuses);

    boolean existsByVehicleId(Long vehicleId);

    boolean existsByCustomerId(Long customerId);

    boolean existsBySalespersonId(Long userId);

    long countByStatus(SaleStatus status);

    @Query("SELECT COALESCE(SUM(s.salePrice - COALESCE(s.discount, 0)), 0) FROM Sale s WHERE s.status = com.showroom.entity.SaleStatus.COMPLETED")
    BigDecimal totalCompletedRevenue();
}
