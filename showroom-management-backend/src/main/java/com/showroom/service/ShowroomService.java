package com.showroom.service;

import com.showroom.dto.ShowroomDto;
import com.showroom.entity.Showroom;
import com.showroom.exception.BadRequestException;
import com.showroom.exception.ConflictException;
import com.showroom.exception.ResourceNotFoundException;
import com.showroom.mapper.EntityMapper;
import com.showroom.repository.ShowroomRepository;
import com.showroom.repository.UserRepository;
import com.showroom.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowroomService {

    private final ShowroomRepository showroomRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public List<ShowroomDto> findAll() {
        return showroomRepository.findAll(Sort.by("name")).stream()
                .map(s -> EntityMapper.toDto(s, vehicleRepository.countByShowroomId(s.getId())))
                .toList();
    }

    public ShowroomDto findById(Long id) {
        Showroom s = getEntity(id);
        return EntityMapper.toDto(s, vehicleRepository.countByShowroomId(id));
    }

    public Showroom getEntity(Long id) {
        return showroomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Showroom", id));
    }

    @Transactional
    public ShowroomDto create(ShowroomDto dto) {
        String code = dto.getCode().trim().toUpperCase();
        if (showroomRepository.existsByCodeIgnoreCase(code)) {
            throw new BadRequestException("Showroom code '" + code + "' already exists");
        }
        Showroom s = new Showroom();
        apply(s, dto, code);
        return EntityMapper.toDto(showroomRepository.save(s), 0);
    }

    @Transactional
    public ShowroomDto update(Long id, ShowroomDto dto) {
        Showroom s = getEntity(id);
        String code = dto.getCode().trim().toUpperCase();
        if (showroomRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new BadRequestException("Showroom code '" + code + "' already exists");
        }
        apply(s, dto, code);
        return EntityMapper.toDto(s, vehicleRepository.countByShowroomId(id));
    }

    @Transactional
    public void delete(Long id) {
        Showroom s = getEntity(id);
        if (vehicleRepository.existsByShowroomId(id)) {
            throw new ConflictException("Showroom still has vehicles; move or delete them first");
        }
        if (userRepository.existsByShowroomId(id)) {
            throw new ConflictException("Showroom still has staff assigned; reassign them first");
        }
        showroomRepository.delete(s);
    }

    private void apply(Showroom s, ShowroomDto dto, String code) {
        s.setName(dto.getName().trim());
        s.setCode(code);
        s.setAddress(EntityMapper.clean(dto.getAddress()));
        s.setCity(EntityMapper.clean(dto.getCity()));
        s.setPhone(EntityMapper.clean(dto.getPhone()));
        s.setEmail(EntityMapper.clean(dto.getEmail()));
    }
}
