package com.showroom.controller;

import com.showroom.dto.ShowroomDto;
import com.showroom.service.ShowroomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/showrooms")
@RequiredArgsConstructor
public class ShowroomController {

    private final ShowroomService showroomService;

    @GetMapping
    public List<ShowroomDto> findAll() {
        return showroomService.findAll();
    }

    @GetMapping("/{id}")
    public ShowroomDto findById(@PathVariable Long id) {
        return showroomService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShowroomDto> create(@Valid @RequestBody ShowroomDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(showroomService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ShowroomDto update(@PathVariable Long id, @Valid @RequestBody ShowroomDto dto) {
        return showroomService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        showroomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
