package com.showroom.repository;

import com.showroom.entity.Vehicle;
import com.showroom.entity.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByVinIgnoreCase(String vin);

    boolean existsByVinIgnoreCaseAndIdNot(String vin, Long id);

    boolean existsByShowroomId(Long showroomId);

    long countByStatus(VehicleStatus status);

    long countByShowroomId(Long showroomId);

    @Query("""
            SELECT v FROM Vehicle v JOIN FETCH v.showroom s
            WHERE (:status IS NULL OR v.status = :status)
              AND (:showroomId IS NULL OR s.id = :showroomId)
              AND (:q IS NULL
                   OR LOWER(v.make) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(v.model) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(v.vin) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY v.createdAt DESC
            """)
    List<Vehicle> search(@Param("status") VehicleStatus status,
                         @Param("showroomId") Long showroomId,
                         @Param("q") String q);
}
