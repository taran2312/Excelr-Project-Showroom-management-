package com.showroom.service;

import com.showroom.dto.ChangePasswordRequest;
import com.showroom.dto.UserDto;
import com.showroom.dto.UserRequest;
import com.showroom.entity.Role;
import com.showroom.entity.Showroom;
import com.showroom.entity.User;
import com.showroom.exception.BadRequestException;
import com.showroom.exception.ConflictException;
import com.showroom.exception.ResourceNotFoundException;
import com.showroom.mapper.EntityMapper;
import com.showroom.repository.SaleRepository;
import com.showroom.repository.ShowroomRepository;
import com.showroom.repository.TestDriveRepository;
import com.showroom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final ShowroomRepository showroomRepository;
    private final SaleRepository saleRepository;
    private final TestDriveRepository testDriveRepository;
    private final PasswordEncoder passwordEncoder;

    /** The authenticated user, loaded from the database. */
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new BadRequestException("No authenticated user");
        }
        return userRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    public UserDto getCurrentUserDto() {
        return EntityMapper.toDto(getCurrentUser());
    }

    public List<UserDto> findAll() {
        return userRepository.findAll(Sort.by("fullName")).stream().map(EntityMapper::toDto).toList();
    }

    public UserDto findById(Long id) {
        return EntityMapper.toDto(getEntity(id));
    }

    public User getEntity(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional
    public UserDto create(UserRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email is already registered");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new BadRequestException("Password is required for new users");
        }
        User user = User.builder()
                .fullName(req.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(EntityMapper.clean(req.getPhone()))
                .role(req.getRole())
                .showroom(resolveShowroom(req.getShowroomId()))
                .active(req.getActive() == null || req.getActive())
                .build();
        return EntityMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto update(Long id, UserRequest req) {
        User user = getEntity(id);
        User current = getCurrentUser();
        String email = req.getEmail().trim().toLowerCase();

        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email is already registered");
        }
        boolean demotingSelf = user.getId().equals(current.getId()) && req.getRole() != Role.ADMIN;
        boolean disablingSelf = user.getId().equals(current.getId()) && Boolean.FALSE.equals(req.getActive());
        if (demotingSelf || disablingSelf) {
            throw new BadRequestException("You cannot remove your own admin role or deactivate yourself");
        }

        user.setFullName(req.getFullName().trim());
        user.setEmail(email);
        user.setPhone(EntityMapper.clean(req.getPhone()));
        user.setRole(req.getRole());
        user.setShowroom(resolveShowroom(req.getShowroomId()));
        if (req.getActive() != null) {
            user.setActive(req.getActive());
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        return EntityMapper.toDto(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = getEntity(id);
        if (user.getId().equals(getCurrentUser().getId())) {
            throw new BadRequestException("You cannot delete your own account");
        }
        if (saleRepository.existsBySalespersonId(id) || testDriveRepository.existsBySalespersonId(id)) {
            throw new ConflictException("User has sales or test drives on record; deactivate the account instead");
        }
        userRepository.delete(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        User user = getCurrentUser();
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
    }

    private Showroom resolveShowroom(Long showroomId) {
        if (showroomId == null) {
            return null;
        }
        return showroomRepository.findById(showroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Showroom", showroomId));
    }
}
