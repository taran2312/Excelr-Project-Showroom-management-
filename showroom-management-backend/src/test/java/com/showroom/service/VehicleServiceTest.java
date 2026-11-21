package com.showroom.service;

import com.showroom.dto.VehicleDto;
import com.showroom.entity.Showroom;
import com.showroom.entity.Vehicle;
import com.showroom.entity.VehicleStatus;
import com.showroom.exception.BadRequestException;
import com.showroom.exception.ConflictException;
import com.showroom.repository.SaleRepository;
import com.showroom.repository.TestDriveRepository;
import com.showroom.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock VehicleRepository vehicleRepository;
    @Mock SaleRepository saleRepository;
    @Mock TestDriveRepository testDriveRepository;
    @Mock ShowroomService showroomService;
    @InjectMocks VehicleService vehicleService;

    private final Showroom showroom = Showroom.builder().id(1L).name("Main").code("MAIN-01").build();

    private VehicleDto dto(VehicleStatus status) {
        return VehicleDto.builder().vin(" vin0000000000001 ").make("Honda").model("City").year(2026)
                .price(new BigDecimal("1500000")).showroomId(1L).status(status).build();
    }

    @Test
    void createNormalisesVinAndDefaultsToAvailable() {
        when(vehicleRepository.existsByVinIgnoreCase("VIN0000000000001")).thenReturn(false);
        when(showroomService.getEntity(1L)).thenReturn(showroom);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleDto result = vehicleService.create(dto(null));

        assertThat(result.getVin()).isEqualTo("VIN0000000000001");
        assertThat(result.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(result.getShowroomName()).isEqualTo("Main");
    }

    @Test
    void createRejectsDuplicateVin() {
        when(vehicleRepository.existsByVinIgnoreCase("VIN0000000000001")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.create(dto(null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createRejectsSaleControlledStatus() {
        assertThatThrownBy(() -> vehicleService.create(dto(VehicleStatus.SOLD)))
                .isInstanceOf(BadRequestException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void manualStatusChangeBlockedWhileSaleIsOpen() {
        Vehicle v = Vehicle.builder().id(5L).status(VehicleStatus.RESERVED).showroom(showroom).build();
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(v));
        when(saleRepository.existsByVehicleIdAndStatusIn(eq(5L), anyList())).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.updateStatus(5L, VehicleStatus.AVAILABLE))
                .isInstanceOf(BadRequestException.class);
        assertThat(v.getStatus()).isEqualTo(VehicleStatus.RESERVED);
    }

    @Test
    void vehicleWithHistoryCannotBeDeleted() {
        Vehicle v = Vehicle.builder().id(5L).showroom(showroom).build();
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(v));
        when(saleRepository.existsByVehicleId(5L)).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.delete(5L)).isInstanceOf(ConflictException.class);
        verify(vehicleRepository, never()).delete(any());
    }
}
