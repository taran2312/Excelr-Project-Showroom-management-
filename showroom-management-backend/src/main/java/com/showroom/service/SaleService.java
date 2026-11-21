package com.showroom.service;

import com.showroom.dto.SaleDto;
import com.showroom.entity.*;
import com.showroom.exception.BadRequestException;
import com.showroom.exception.ResourceNotFoundException;
import com.showroom.mapper.EntityMapper;
import com.showroom.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Sale lifecycle and its effect on vehicle inventory:
 * <pre>
 *   create            -> sale PENDING,   vehicle RESERVED
 *   PENDING->COMPLETED -> vehicle SOLD
 *   PENDING->CANCELLED -> vehicle AVAILABLE
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SaleService {

    private static final DateTimeFormatter INVOICE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SaleRepository saleRepository;
    private final VehicleService vehicleService;
    private final CustomerService customerService;
    private final UserService userService;

    public List<SaleDto> findAll(SaleStatus status) {
        List<Sale> sales = status == null
                ? saleRepository.findAllByOrderByCreatedAtDesc()
                : saleRepository.findByStatusOrderByCreatedAtDesc(status);
        return sales.stream().map(EntityMapper::toDto).toList();
    }

    public SaleDto findById(Long id) {
        return EntityMapper.toDto(getEntity(id));
    }

    private Sale getEntity(Long id) {
        return saleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sale", id));
    }

    @Transactional
    public SaleDto create(SaleDto dto) {
        Vehicle vehicle = vehicleService.getEntity(dto.getVehicleId());
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new BadRequestException("Vehicle is not available for sale (status: " + vehicle.getStatus() + ")");
        }
        validateAmounts(dto);

        Sale sale = Sale.builder()
                .invoiceNumber(generateInvoiceNumber())
                .vehicle(vehicle)
                .customer(customerService.getEntity(dto.getCustomerId()))
                .salesperson(resolveSalesperson(dto.getSalespersonId()))
                .salePrice(dto.getSalePrice())
                .discount(dto.getDiscount() == null ? BigDecimal.ZERO : dto.getDiscount())
                .paymentMethod(dto.getPaymentMethod())
                .status(SaleStatus.PENDING)
                .saleDate(dto.getSaleDate() == null ? LocalDate.now() : dto.getSaleDate())
                .notes(EntityMapper.clean(dto.getNotes()))
                .build();

        vehicle.setStatus(VehicleStatus.RESERVED);
        return EntityMapper.toDto(saleRepository.save(sale));
    }

    /** Only PENDING sales can be edited; the vehicle cannot be swapped (cancel and re-create instead). */
    @Transactional
    public SaleDto update(Long id, SaleDto dto) {
        Sale sale = getEntity(id);
        if (sale.getStatus() != SaleStatus.PENDING) {
            throw new BadRequestException("Only PENDING sales can be edited");
        }
        if (!sale.getVehicle().getId().equals(dto.getVehicleId())) {
            throw new BadRequestException("Vehicle cannot be changed; cancel this sale and create a new one");
        }
        validateAmounts(dto);
        sale.setCustomer(customerService.getEntity(dto.getCustomerId()));
        if (dto.getSalespersonId() != null) {
            sale.setSalesperson(userService.getEntity(dto.getSalespersonId()));
        }
        sale.setSalePrice(dto.getSalePrice());
        sale.setDiscount(dto.getDiscount() == null ? BigDecimal.ZERO : dto.getDiscount());
        sale.setPaymentMethod(dto.getPaymentMethod());
        if (dto.getSaleDate() != null) {
            sale.setSaleDate(dto.getSaleDate());
        }
        sale.setNotes(EntityMapper.clean(dto.getNotes()));
        return EntityMapper.toDto(sale);
    }

    @Transactional
    public SaleDto updateStatus(Long id, SaleStatus target) {
        Sale sale = getEntity(id);
        if (sale.getStatus() != SaleStatus.PENDING) {
            throw new BadRequestException("Sale is already " + sale.getStatus() + " and cannot change status");
        }
        switch (target) {
            case COMPLETED -> sale.getVehicle().setStatus(VehicleStatus.SOLD);
            case CANCELLED -> sale.getVehicle().setStatus(VehicleStatus.AVAILABLE);
            case PENDING -> throw new BadRequestException("Sale is already PENDING");
        }
        sale.setStatus(target);
        return EntityMapper.toDto(sale);
    }

    /** Deleting a sale that still holds a vehicle releases it; completed sales are kept for records. */
    @Transactional
    public void delete(Long id) {
        Sale sale = getEntity(id);
        if (sale.getStatus() == SaleStatus.COMPLETED) {
            throw new BadRequestException("Completed sales cannot be deleted");
        }
        if (sale.getStatus() == SaleStatus.PENDING) {
            sale.getVehicle().setStatus(VehicleStatus.AVAILABLE);
        }
        saleRepository.delete(sale);
    }

    private User resolveSalesperson(Long salespersonId) {
        return salespersonId == null ? userService.getCurrentUser() : userService.getEntity(salespersonId);
    }

    private void validateAmounts(SaleDto dto) {
        BigDecimal discount = dto.getDiscount() == null ? BigDecimal.ZERO : dto.getDiscount();
        if (discount.compareTo(dto.getSalePrice()) > 0) {
            throw new BadRequestException("Discount cannot exceed the sale price");
        }
    }

    private String generateInvoiceNumber() {
        return "INV-" + LocalDate.now().format(INVOICE_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
