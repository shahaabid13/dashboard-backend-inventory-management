package com.inventory.msp.incident.controller;

import com.inventory.msp.incident.dto.AppUserResponse;
import com.inventory.msp.incident.dto.CreateUserRequest;
import com.inventory.msp.incident.dto.UpdateUserRequest;
import com.inventory.msp.incident.service.IncidentMapper;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.UserRole;
import com.inventory.msp.repository.UserRepository;
import com.inventory.msp.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final IncidentMapper incidentMapper;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        String roleValue = request.getRole() == null ? null : request.getRole().trim();
        if (roleValue == null || roleValue.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Role is required"));
        }

        UserRole role;
        try {
            role = UserRole.valueOf(roleValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid role: " + roleValue));
        }

        // Validate FIELD_PERSON requirements
        if (role == UserRole.FIELD_PERSON) {
            if (request.getName() == null || request.getName().isBlank()) {
               return ResponseEntity.badRequest()
                       .body(new ErrorResponse("Name is required for FIELD_PERSON role"));
            }
            if (request.getPhone() == null || request.getPhone().isBlank()) {
               return ResponseEntity.badRequest()
                       .body(new ErrorResponse("Phone is required for FIELD_PERSON role"));
            }
        }

        AppUser user = authService.createUser(
               request.getUsername(),
               request.getPassword(),
               role,
               request.getAgencyName(),
               request.getName(),
               request.getPhone(),
               request.getFullName(),
               request.getEmail()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentMapper.toAppUserResponse(user));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppUserResponse>> getAllUsers() {
        List<AppUser> users = userRepository.findAll();
        return ResponseEntity.ok(users.stream()
                .map(incidentMapper::toAppUserResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/assignable")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public ResponseEntity<List<AppUserResponse>> getAssignableUsers() {
        List<AppUser> users = userRepository.findAll();
        return ResponseEntity.ok(users.stream()
                .map(incidentMapper::toAppUserResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppUserResponse> getUser(@PathVariable Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(incidentMapper.toAppUserResponse(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppUserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        AppUser user = authService.updateUser(id,
                request.getRole(),
                request.getAgencyName(),
                request.getPassword(),
                request.getFullName(),
                request.getEmail(),
                request.getPhone());
        return ResponseEntity.ok(incidentMapper.toAppUserResponse(user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Simple error response DTO
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String message;
    }
}

