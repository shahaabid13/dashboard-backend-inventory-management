package com.inventory.msp.util;

import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.UserRole;

import com.inventory.msp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@RequiredArgsConstructor
public class UserBootstrap {

    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.username:admin}")
    private String adminUsername;

    @Value("${app.bootstrap.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.bootstrap.admin.full-name:Administrator}")
    private String adminFullName;

    @Bean
    public CommandLineRunner initUsers(UserRepository repo) {
        return args -> {

            // ✅ ADMIN
            if (!repo.existsByUsername(adminUsername)) {
                AppUser admin = AppUser.builder()
                        .username(adminUsername)
                        .password(passwordEncoder.encode(adminPassword))
                        .role(UserRole.ADMIN)
                        .fullName(adminFullName)
                        .agencyName(null)
                        .build();
                repo.save(admin);
                System.out.println("Default ADMIN created: " + adminUsername);
            }

            // ✅ AGENCY
            if (!repo.existsByUsername("agency1")) {
                AppUser agency = AppUser.builder()
                        .username("agency1")
                        .password(passwordEncoder.encode("agency123"))
                        .role(UserRole.AGENCY)
                        .agencyName("Default Agency")
                        .build();
                repo.save(agency);
                System.out.println("✅ Default AGENCY created");
            }

            // ✅ VIEWER
            if (!repo.existsByUsername("viewer1")) {
                AppUser viewer = AppUser.builder()
                        .username("viewer1")
                        .password(passwordEncoder.encode("viewer123"))
                        .role(UserRole.VIEWER)
                        .agencyName(null)
                        .build();
                repo.save(viewer);
                System.out.println("✅ Default VIEWER created");
            }
        };
    }
}
