package com.showroom.controller;

import com.showroom.dto.StatusUpdateRequest;
import com.showroom.dto.VehicleDto;
import com.showroom.entity.VehicleStatus;
import com.showroom.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    /** GET /api/vehicles?status=AVAILABLE&showroomId=1&q=civic */
    @GetMapping
    public List<VehicleDto> search(@RequestParam(required = false) VehicleStatus status,
                                   @RequestParam(required = false) Long showroomId,
                                   @RequestParam(required = false) String q) {
        return vehicleService.search(status, showroomId, q);
    }

    @GetMapping("/{id}")
    public VehicleDto findById(@PathVariable Long id) {
        return vehicleService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<VehicleDto> create(@Valid @RequestBody VehicleDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public VehicleDto update(@PathVariable Long id, @Valid @RequestBody VehicleDto dto) {
        return vehicleService.update(id, dto);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public VehicleDto updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return vehicleService.updateStatus(id, EnumParser.parse(VehicleStatus.class, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vehicleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
