package com.showroom.service;

import com.showroom.dto.VehicleDto;
import com.showroom.entity.SaleStatus;
import com.showroom.entity.Vehicle;
import com.showroom.entity.VehicleStatus;
import com.showroom.exception.BadRequestException;
import com.showroom.exception.ConflictException;
import com.showroom.exception.ResourceNotFoundException;
import com.showroom.mapper.EntityMapper;
import com.showroom.repository.SaleRepository;
import com.showroom.repository.TestDriveRepository;
import com.showroom.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final SaleRepository saleRepository;
    private final TestDriveRepository testDriveRepository;
    private final ShowroomService showroomService;

    public List<VehicleDto> search(VehicleStatus status, Long showroomId, String q) {
        return vehicleRepository.search(status, showroomId, EntityMapper.clean(q)).stream()
                .map(EntityMapper::toDto)
                .toList();
    }

    public VehicleDto findById(Long id) {
        return EntityMapper.toDto(getEntity(id));
    }

    public Vehicle getEntity(Long id) {
        return vehicleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
    }

    @Transactional
    public VehicleDto create(VehicleDto dto) {
        String vin = dto.getVin().trim().toUpperCase();
        if (vehicleRepository.existsByVinIgnoreCase(vin)) {
            throw new BadRequestException("A vehicle with VIN " + vin + " already exists");
        }
        VehicleStatus status = dto.getStatus() == null ? VehicleStatus.AVAILABLE : dto.getStatus();
        if (status == VehicleStatus.SOLD || status == VehicleStatus.RESERVED) {
            throw new BadRequestException("New vehicles must be AVAILABLE or IN_SERVICE; RESERVED/SOLD are set by sales");
        }
        Vehicle v = new Vehicle();
        apply(v, dto, vin);
        v.setStatus(status);
        return EntityMapper.toDto(vehicleRepository.save(v));
    }

    @Transactional
    public VehicleDto update(Long id, VehicleDto dto) {
        Vehicle v = getEntity(id);
        String vin = dto.getVin().trim().toUpperCase();
        if (vehicleRepository.existsByVinIgnoreCaseAndIdNot(vin, id)) {
            throw new BadRequestException("A vehicle with VIN " + vin + " already exists");
        }
        apply(v, dto, vin);
        if (dto.getStatus() != null && dto.getStatus() != v.getStatus()) {
            changeStatus(v, dto.getStatus());
        }
        return EntityMapper.toDto(v);
    }

    /** Manual status change (e.g. send to service). Sale-driven states are protected. */
    @Transactional
    public VehicleDto updateStatus(Long id, VehicleStatus status) {
        Vehicle v = getEntity(id);
        changeStatus(v, status);
        return EntityMapper.toDto(v);
    }

    @Transactional
    public void delete(Long id) {
        Vehicle v = getEntity(id);
        if (saleRepository.existsByVehicleId(id) || testDriveRepository.existsByVehicleId(id)) {
            throw new ConflictException("Vehicle has sales or test drives on record and cannot be deleted");
        }
        vehicleRepository.delete(v);
    }

    private void changeStatus(Vehicle v, VehicleStatus target) {
        boolean hasOpenSale = saleRepository.existsByVehicleIdAndStatusIn(
                v.getId(), List.of(SaleStatus.PENDING, SaleStatus.COMPLETED));
        if (hasOpenSale) {
            throw new BadRequestException("Vehicle status is controlled by its active sale");
        }
        if (target == VehicleStatus.SOLD || target == VehicleStatus.RESERVED) {
            throw new BadRequestException("RESERVED/SOLD are set automatically by creating/completing a sale");
        }
        v.setStatus(target);
    }

    private void apply(Vehicle v, VehicleDto dto, String vin) {
        v.setVin(vin);
        v.setMake(dto.getMake().trim());
        v.setModel(dto.getModel().trim());
        v.setVariant(EntityMapper.clean(dto.getVariant()));
        v.setYear(dto.getYear());
        v.setColor(EntityMapper.clean(dto.getColor()));
        v.setFuelType(EntityMapper.clean(dto.getFuelType()));
        v.setTransmission(EntityMapper.clean(dto.getTransmission()));
        v.setPrice(dto.getPrice());
        v.setMileage(dto.getMileage());
        v.setDescription(EntityMapper.clean(dto.getDescription()));
        v.setShowroom(showroomService.getEntity(dto.getShowroomId()));
    }
}
