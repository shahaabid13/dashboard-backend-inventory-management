package com.inventory.msp.controller;

import com.inventory.msp.dto.AuthRequest;
import com.inventory.msp.dto.AuthResponse;
import com.inventory.msp.model.UserRole;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.security.JwtUtil;
import com.inventory.msp.services.AuthService;
import com.inventory.msp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        String password = req.getPassword();

        if (username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        try {
            String token = authService.login(username, password);
            String subject = jwtUtil.getUsernameFromToken(token);
            AppUser user = userRepository.findByUsername(subject)
                    .orElseThrow(() -> new RuntimeException("Authenticated user profile missing"));
            return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole().name()));
        } catch (Exception ex) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    // ADMIN-only in SecurityConfig; but you can restrict additionally
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@RequestParam String username,
                                        @RequestParam String password,
                                        @RequestParam String role,
                                        @RequestParam(required = false) String agencyName,
                                        @RequestParam(required = false) String name,
                                        @RequestParam(required = false) String phone,
                                        @RequestParam(required = false) String fullName,
                                        @RequestParam(required = false) String email) {

        UserRole r = UserRole.valueOf(role.toUpperCase());

        if (r == UserRole.FIELD_PERSON) {
            if (phone == null || phone.isBlank()) {
                return ResponseEntity.badRequest().body("Phone is required for FIELD_PERSON role");
            }
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body("Email is required for FIELD_PERSON role");
            }
        }

        AppUser u = authService.createUser(username, password, r, agencyName, name, phone, fullName, email);
        return ResponseEntity.ok(u);
    }
}