package com.inventory.msp.services;

import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.UserRole;
import com.inventory.msp.incident.model.FieldPerson;
import com.inventory.msp.incident.repository.FieldPersonRepository;
import com.inventory.msp.repository.UserRepository;
import com.inventory.msp.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.inventory.msp.security.LdapAuthService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final FieldPersonRepository fieldPersonRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LdapAuthService ldapAuthService;

    /**
     * Hybrid login: local DB (bcrypt) first, then fallback to LDAP. On successful LDAP login,
     * ensure an AppUser row exists (create if missing) with password=null and a default role.
     */
    @Transactional
    public String login(String username, String password) {
        if (username == null) username = "";
        String trimmed = username.trim();
        if (trimmed.isBlank() || password == null || password.isBlank()) {
            throw new RuntimeException("Invalid credentials");
        }

        // 1) Try local DB auth if user exists and has a password set
        AppUser local = userRepository.findByUsername(trimmed).orElse(null);
        if (local != null && local.getPassword() != null && !local.getPassword().isBlank()) {
            if (!passwordEncoder.matches(password, local.getPassword())) {
                throw new RuntimeException("Invalid credentials");
            }
            return jwtUtil.generateToken(local.getUsername(), local.getRole().name());
        }

        // 2) Fallback to LDAP authenticate
        var maybe = ldapAuthService.authenticate(trimmed, password);
        if (maybe.isEmpty()) {
            throw new RuntimeException("Invalid credentials");
        }

        LdapAuthService.AdLoginResult ad = maybe.get();
        String normalizedUsername = ad.username();
        String displayName = ad.displayName();
        String email = ad.email();
        String mappedRoleStr = ad.role();

        // 3) Ensure AppUser exists locally (create if needed). Password remains null for AD users.
        AppUser user = userRepository.findByUsername(normalizedUsername).orElse(null);
        if (user == null) {
            UserRole role = UserRole.VIEWER; // safe default
            if (mappedRoleStr != null) {
                try {
                    role = UserRole.valueOf(mappedRoleStr.toUpperCase());
                } catch (Exception ignored) { }
            }
            AppUser u = AppUser.builder()
                    .username(normalizedUsername)
                    .password(null)
                    .role(role)
                    .fullName(displayName)
                    .email(email)
                    .build();
            user = userRepository.save(u);
            // create linked profiles if role requires them (syncRoleProfile is safe)
            syncRoleProfile(user, displayName, null);
        } else {
            // Optionally update profile info if missing
            boolean changed = false;
            if ((user.getFullName() == null || user.getFullName().isBlank()) && displayName != null) {
                user.setFullName(displayName); changed = true;
            }
            if ((user.getEmail() == null || user.getEmail().isBlank()) && email != null) {
                user.setEmail(email); changed = true;
            }
            if (changed) userRepository.save(user);
        }
        return jwtUtil.generateToken(user.getUsername(), user.getRole().name());
    }

    @Transactional
    public AppUser createUser(String username, String rawPassword, UserRole role, String agencyName, String name, String phone, String fullName, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("User already exists");
        }

        if (role == UserRole.FIELD_PERSON) {
            if (phone == null || phone.isBlank()) {
                throw new RuntimeException("Phone is required for FIELD_PERSON role");
            }
            if (email == null || email.isBlank()) {
                throw new RuntimeException("Email is required for FIELD_PERSON role");
            }
        }

        AppUser user = AppUser.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .agencyName(agencyName)
                .fullName(fullName != null ? fullName : name)
                .email(email)
                .phone(phone)
                .build();
        AppUser savedUser = userRepository.save(user);

        // Ensures any role-specific linked profile (currently only FIELD_PERSON) exists.
        syncRoleProfile(savedUser, name != null ? name : fullName, phone);

        return savedUser;
    }

    @Transactional
    public AppUser updateUser(Long id, String role, String agencyName, String rawPassword, String fullName, String email, String phone) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (role != null) {
            user.setRole(UserRole.valueOf(role.toUpperCase()));
        }
        if (agencyName != null) {
            user.setAgencyName(agencyName);
        }
        if (rawPassword != null && !rawPassword.isEmpty()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }

        if (fullName != null) user.setFullName(fullName);
        if (email != null) user.setEmail(email);
        if (phone != null) user.setPhone(phone);

        AppUser saved = userRepository.save(user);

        // Ensures a FieldPerson profile exists/stays in sync whenever the role
        // is (or becomes) FIELD_PERSON — covers role changes made after creation.
        syncRoleProfile(saved, fullName, phone);

        return saved;
    }

    public void deleteUser(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
    }

    /**
     * Single source of truth for keeping role-specific linked profiles in sync
     * with an AppUser's current role. Currently only FIELD_PERSON has a linked
     * profile table (FieldPerson); other roles (REVIEWER, ADMIN, SUPPORT_ENGINEER,
     * VIEWER, AGENCY) are read directly off AppUser and need no linked row.
     *
     * Safe to call after ANY create or update, from ANY entry point (admin-created
     * users, AD JIT-provisioned users, role changes) — it creates the profile if
     * missing, updates name/phone if one already exists, and does nothing for
     * roles that don't need a linked profile.
     */
    public void syncRoleProfile(AppUser user, String preferredName, String phone) {
        if (user.getRole() != UserRole.FIELD_PERSON) {
            return;
        }

        FieldPerson fieldPerson = fieldPersonRepository.findByUserId(user.getId()).orElse(null);
        String resolvedName = (preferredName != null && !preferredName.isBlank())
                ? preferredName
                : (user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getUsername());
        String resolvedPhone = (phone != null && !phone.isBlank()) ? phone : user.getPhone();

        if (fieldPerson == null) {
            fieldPerson = FieldPerson.builder()
                    .name(resolvedName)
                    .phone(resolvedPhone)
                    .role("Field Person")
                    .active(true)
                    .user(user)
                    .build();
            fieldPersonRepository.save(fieldPerson);
        } else {
            boolean changed = false;
            if (resolvedName != null && !resolvedName.equals(fieldPerson.getName())) {
                fieldPerson.setName(resolvedName);
                changed = true;
            }
            if (resolvedPhone != null && !resolvedPhone.equals(fieldPerson.getPhone())) {
                fieldPerson.setPhone(resolvedPhone);
                changed = true;
            }
            if (changed) {
                fieldPersonRepository.save(fieldPerson);
            }
        }
    }
}