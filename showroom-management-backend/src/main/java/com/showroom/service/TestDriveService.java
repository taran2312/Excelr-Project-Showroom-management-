package com.showroom.service;

import com.showroom.dto.TestDriveDto;
import com.showroom.entity.TestDrive;
import com.showroom.entity.TestDriveStatus;
import com.showroom.entity.Vehicle;
import com.showroom.entity.VehicleStatus;
import com.showroom.exception.BadRequestException;
import com.showroom.exception.ResourceNotFoundException;
import com.showroom.mapper.EntityMapper;
import com.showroom.repository.TestDriveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestDriveService {

    private final TestDriveRepository testDriveRepository;
    private final VehicleService vehicleService;
    private final CustomerService customerService;
    private final UserService userService;

    public List<TestDriveDto> findAll(TestDriveStatus status) {
        List<TestDrive> list = status == null
                ? testDriveRepository.findAllByOrderByScheduledAtDesc()
                : testDriveRepository.findByStatusOrderByScheduledAtDesc(status);
        return list.stream().map(EntityMapper::toDto).toList();
    }

    public TestDriveDto findById(Long id) {
        return EntityMapper.toDto(getEntity(id));
    }

    private TestDrive getEntity(Long id) {
        return testDriveRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Test drive", id));
    }

    @Transactional
    public TestDriveDto create(TestDriveDto dto) {
        Vehicle vehicle = vehicleService.getEntity(dto.getVehicleId());
        if (vehicle.getStatus() == VehicleStatus.SOLD || vehicle.getStatus() == VehicleStatus.IN_SERVICE) {
            throw new BadRequestException("Vehicle is " + vehicle.getStatus() + " and cannot be test driven");
        }
        TestDrive td = TestDrive.builder()
                .vehicle(vehicle)
                .customer(customerService.getEntity(dto.getCustomerId()))
                .salesperson(dto.getSalespersonId() == null
                        ? userService.getCurrentUser()
                        : userService.getEntity(dto.getSalespersonId()))
                .scheduledAt(dto.getScheduledAt())
                .status(TestDriveStatus.SCHEDULED)
                .feedback(EntityMapper.clean(dto.getFeedback()))
                .build();
        return EntityMapper.toDto(testDriveRepository.save(td));
    }

    @Transactional
    public TestDriveDto update(Long id, TestDriveDto dto) {
        TestDrive td = getEntity(id);
        if (td.getStatus() != TestDriveStatus.SCHEDULED) {
            // Closed test drives: only feedback can still be edited.
            td.setFeedback(EntityMapper.clean(dto.getFeedback()));
            return EntityMapper.toDto(td);
        }
        td.setVehicle(vehicleService.getEntity(dto.getVehicleId()));
        td.setCustomer(customerService.getEntity(dto.getCustomerId()));
        if (dto.getSalespersonId() != null) {
            td.setSalesperson(userService.getEntity(dto.getSalespersonId()));
        }
        td.setScheduledAt(dto.getScheduledAt());
        td.setFeedback(EntityMapper.clean(dto.getFeedback()));
        return EntityMapper.toDto(td);
    }

    @Transactional
    public TestDriveDto updateStatus(Long id, TestDriveStatus target, String feedback) {
        TestDrive td = getEntity(id);
        if (td.getStatus() != TestDriveStatus.SCHEDULED) {
            throw new BadRequestException("Test drive is already " + td.getStatus());
        }
        if (target == TestDriveStatus.SCHEDULED) {
            throw new BadRequestException("Test drive is already SCHEDULED");
        }
        td.setStatus(target);
        if (feedback != null && !feedback.isBlank()) {
            td.setFeedback(feedback.trim());
        }
        return EntityMapper.toDto(td);
    }

    @Transactional
    public void delete(Long id) {
        testDriveRepository.delete(getEntity(id));
    }
}
