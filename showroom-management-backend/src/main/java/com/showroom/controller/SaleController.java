package com.showroom.controller;

import com.showroom.dto.SaleDto;
import com.showroom.dto.StatusUpdateRequest;
import com.showroom.entity.SaleStatus;
import com.showroom.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    public List<SaleDto> findAll(@RequestParam(required = false) SaleStatus status) {
        return saleService.findAll(status);
    }

    @GetMapping("/{id}")
    public SaleDto findById(@PathVariable Long id) {
        return saleService.findById(id);
    }

    @PostMapping
    public ResponseEntity<SaleDto> create(@Valid @RequestBody SaleDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.create(dto));
    }

    @PutMapping("/{id}")
    public SaleDto update(@PathVariable Long id, @Valid @RequestBody SaleDto dto) {
        return saleService.update(id, dto);
    }

    /** Approve (COMPLETED) or cancel (CANCELLED) a pending sale. */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public SaleDto updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return saleService.updateStatus(id, EnumParser.parse(SaleStatus.class, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        saleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
