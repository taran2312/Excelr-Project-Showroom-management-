package com.showroom.controller;

import com.showroom.dto.StatusUpdateRequest;
import com.showroom.dto.TestDriveDto;
import com.showroom.entity.TestDriveStatus;
import com.showroom.service.TestDriveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test-drives")
@RequiredArgsConstructor
public class TestDriveController {

    private final TestDriveService testDriveService;

    @GetMapping
    public List<TestDriveDto> findAll(@RequestParam(required = false) TestDriveStatus status) {
        return testDriveService.findAll(status);
    }

    @GetMapping("/{id}")
    public TestDriveDto findById(@PathVariable Long id) {
        return testDriveService.findById(id);
    }

    @PostMapping
    public ResponseEntity<TestDriveDto> create(@Valid @RequestBody TestDriveDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(testDriveService.create(dto));
    }

    @PutMapping("/{id}")
    public TestDriveDto update(@PathVariable Long id, @Valid @RequestBody TestDriveDto dto) {
        return testDriveService.update(id, dto);
    }

    @PatchMapping("/{id}/status")
    public TestDriveDto updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return testDriveService.updateStatus(id,
                EnumParser.parse(TestDriveStatus.class, request.getStatus()), request.getNote());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        testDriveService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
