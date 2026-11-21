package com.showroom.config;

import com.showroom.entity.Role;
import com.showroom.entity.Showroom;
import com.showroom.entity.User;
import com.showroom.repository.ShowroomRepository;
import com.showroom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a default ADMIN account and a sample showroom on first start (empty database only).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ShowroomRepository showroomRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-email}")
    private String adminEmail;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        Showroom main = showroomRepository.save(Showroom.builder()
                .name("Main Showroom")
                .code("MAIN-01")
                .address("100 Market Street")
                .city("Mumbai")
                .phone("+91 22 0000 0000")
                .email("main@showroom.com")
                .build());

        userRepository.save(User.builder()
                .fullName("System Administrator")
                .email(adminEmail.toLowerCase())
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .showroom(main)
                .active(true)
                .build());

        log.info("Seeded default admin '{}' and showroom '{}'. Change the admin password after first login.",
                adminEmail, main.getName());
    }
}
