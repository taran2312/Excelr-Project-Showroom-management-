package com.showroom.service;

import com.showroom.dto.SaleDto;
import com.showroom.entity.*;
import com.showroom.exception.BadRequestException;
import com.showroom.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Sale lifecycle rules and their effect on vehicle status. */
@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock SaleRepository saleRepository;
    @Mock VehicleService vehicleService;
    @Mock CustomerService customerService;
    @Mock UserService userService;
    @InjectMocks SaleService saleService;

    private Vehicle vehicle;
    private Customer customer;
    private User seller;

    @BeforeEach
    void setUp() {
        Showroom showroom = Showroom.builder().id(1L).name("Main").code("MAIN-01").build();
        vehicle = Vehicle.builder().id(10L).vin("VIN0000000000001").make("Honda").model("City").year(2026)
                .price(new BigDecimal("1500000")).status(VehicleStatus.AVAILABLE).showroom(showroom).build();
        customer = Customer.builder().id(20L).fullName("Asha Rao").phone("9000000001").build();
        seller = User.builder().id(30L).fullName("Sam Sales").email("sam@showroom.com").role(Role.SALES).build();
    }

    private SaleDto request(String price, String discount) {
        return SaleDto.builder().vehicleId(10L).customerId(20L).salePrice(new BigDecimal(price))
                .discount(discount == null ? null : new BigDecimal(discount))
                .paymentMethod(PaymentMethod.FINANCE).build();
    }

    private Sale pendingSale() {
        return Sale.builder().id(99L).invoiceNumber("INV-1").vehicle(vehicle).customer(customer).salesperson(seller)
                .salePrice(new BigDecimal("1500000")).paymentMethod(PaymentMethod.CASH)
                .status(SaleStatus.PENDING).saleDate(LocalDate.now()).build();
    }

    @Test
    void createReservesVehicleAndDefaultsToCurrentUser() {
        when(vehicleService.getEntity(10L)).thenReturn(vehicle);
        when(customerService.getEntity(20L)).thenReturn(customer);
        when(userService.getCurrentUser()).thenReturn(seller);
        when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        SaleDto result = saleService.create(request("1500000", "50000"));

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.RESERVED);
        assertThat(result.getStatus()).isEqualTo(SaleStatus.PENDING);
        assertThat(result.getFinalAmount()).isEqualByComparingTo("1450000");
        assertThat(result.getSalespersonName()).isEqualTo("Sam Sales");
        assertThat(result.getSaleDate()).isEqualTo(LocalDate.now());
        assertThat(result.getInvoiceNumber()).startsWith("INV-");
    }

    @Test
    void createRejectsVehicleThatIsNotAvailable() {
        vehicle.setStatus(VehicleStatus.RESERVED);
        when(vehicleService.getEntity(10L)).thenReturn(vehicle);

        assertThatThrownBy(() -> saleService.create(request("1500000", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not available");
        verify(saleRepository, never()).save(any());
    }

    @Test
    void createRejectsDiscountAboveSalePrice() {
        when(vehicleService.getEntity(10L)).thenReturn(vehicle);

        assertThatThrownBy(() -> saleService.create(request("1000", "2000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Discount");
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    void completingSaleMarksVehicleSold() {
        vehicle.setStatus(VehicleStatus.RESERVED);
        Sale sale = pendingSale();
        when(saleRepository.findById(99L)).thenReturn(Optional.of(sale));

        SaleDto result = saleService.updateStatus(99L, SaleStatus.COMPLETED);

        assertThat(result.getStatus()).isEqualTo(SaleStatus.COMPLETED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.SOLD);
    }

    @Test
    void cancellingSaleReleasesVehicle() {
        vehicle.setStatus(VehicleStatus.RESERVED);
        when(saleRepository.findById(99L)).thenReturn(Optional.of(pendingSale()));

        saleService.updateStatus(99L, SaleStatus.CANCELLED);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    void closedSaleCannotChangeStatus() {
        Sale sale = pendingSale();
        sale.setStatus(SaleStatus.COMPLETED);
        vehicle.setStatus(VehicleStatus.SOLD);
        when(saleRepository.findById(99L)).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> saleService.updateStatus(99L, SaleStatus.CANCELLED))
                .isInstanceOf(BadRequestException.class);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.SOLD);
    }

    @Test
    void deletingPendingSaleReleasesVehicle() {
        vehicle.setStatus(VehicleStatus.RESERVED);
        Sale sale = pendingSale();
        when(saleRepository.findById(99L)).thenReturn(Optional.of(sale));

        saleService.delete(99L);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        verify(saleRepository).delete(sale);
    }

    @Test
    void completedSaleCannotBeDeleted() {
        Sale sale = pendingSale();
        sale.setStatus(SaleStatus.COMPLETED);
        when(saleRepository.findById(99L)).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> saleService.delete(99L)).isInstanceOf(BadRequestException.class);
        verify(saleRepository, never()).delete(any());
    }

    @Test
    void pendingSaleCannotSwapVehicle() {
        when(saleRepository.findById(99L)).thenReturn(Optional.of(pendingSale()));
        SaleDto change = request("1500000", null);
        change.setVehicleId(11L);

        assertThatThrownBy(() -> saleService.update(99L, change))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Vehicle cannot be changed");
    }
}
