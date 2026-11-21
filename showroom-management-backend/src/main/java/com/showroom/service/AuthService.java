package com.showroom.service;

import com.showroom.config.security.JwtUtil;
import com.showroom.dto.AuthResponse;
import com.showroom.dto.LoginRequest;
import com.showroom.dto.RegisterRequest;
import com.showroom.entity.Role;
import com.showroom.entity.User;
import com.showroom.exception.BadRequestException;
import com.showroom.mapper.EntityMapper;
import com.showroom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        // Throws BadCredentialsException / DisabledException on failure (handled globally).
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        return buildResponse(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email is already registered");
        }
        User user = userRepository.save(User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(EntityMapper.clean(request.getPhone()))
                .role(Role.SALES)
                .active(true)
                .build());
        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtUtil.generateToken(user.getEmail(),
                Map.of("role", user.getRole().name(), "uid", user.getId()));
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(jwtUtil.getExpirationMs())
                .user(EntityMapper.toDto(user))
                .build();
    }
}
