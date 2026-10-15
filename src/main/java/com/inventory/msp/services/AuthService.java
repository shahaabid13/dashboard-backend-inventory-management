package com.inventory.msp.services;

import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.UserRole;
import com.inventory.msp.incident.model.FieldPerson;
import com.inventory.msp.incident.repository.FieldPersonRepository;
import com.inventory.msp.repository.UserRepository;
import com.inventory.msp.security.JwtUtil;
import com.inventory.msp.security.LdapUserProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.inventory.msp.security.LdapAuthService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final FieldPersonRepository fieldPersonRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LdapAuthService ldapAuthService;
    private final LdapUserProvisioningService ldapUserProvisioningService; // NEW

    @Transactional
    public String login(String username, String password) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (normalizedUsername.isBlank() || password == null || password.isBlank()) {
            throw new RuntimeException("Invalid credentials");
        }

        java.util.Optional<com.inventory.msp.security.LdapAuthService.AdLoginResult> adLogin =
                ldapAuthService.authenticate(normalizedUsername, password);
        if (adLogin.isEmpty()) {
            throw new RuntimeException("Invalid credentials");
        }

        com.inventory.msp.security.LdapAuthService.AdLoginResult loginResult = adLogin.get();
        AppUser user = userRepository.findByUsername(loginResult.username())
                .orElseGet(() -> {
                    AppUser newUser = AppUser.builder()
                            .username(loginResult.username())
                            .password(null)
                            .role(UserRole.valueOf(loginResult.role()))
                            .fullName(loginResult.displayName())
                            .email(loginResult.email())
                            .build();
                    return userRepository.save(newUser);
                });

        if (user.getRole() == null) {
            user.setRole(UserRole.valueOf(loginResult.role()));
        }
        if (user.getFullName() == null && loginResult.displayName() != null) {
            user.setFullName(loginResult.displayName());
        }
        if (user.getEmail() == null && loginResult.email() != null) {
            user.setEmail(loginResult.email());
        }
        userRepository.save(user);
        syncRoleProfile(user, user.getFullName(), user.getPhone());

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

        String resolvedFullName = fullName != null ? fullName : name;

        // 1) Create the user in Active Directory FIRST. AD is now the source of
        // truth for credentials — if this fails, we never touch the local DB.
        boolean createdInDirectory = ldapUserProvisioningService.createAdUser(
                username,
                rawPassword,
                resolvedFullName,
                email,
                phone,
                role.name()
        );

        try {
            // 2) Mirror into local DB — password intentionally left NULL.
            // This matches the same pattern already used for AD users created
            // via the login() JIT-provisioning path above.
            AppUser user = AppUser.builder()
                    .username(username)
                    .password(null)
                    .role(role)
                    .agencyName(agencyName)
                    .fullName(resolvedFullName)
                    .email(email)
                    .phone(phone)
                    .build();
            AppUser savedUser = userRepository.save(user);

            // Ensures any role-specific linked profile (currently only FIELD_PERSON) exists.
            syncRoleProfile(savedUser, name != null ? name : fullName, phone);

            return savedUser;

        } catch (Exception dbException) {
            // DB failed after AD succeeded — roll back the AD account so the two
            // systems don't drift (an AD account with no corresponding local row).
            log.error("DB save failed after AD user '{}' was created. Rolling back AD account.",
                    username, dbException);
            if (createdInDirectory) {
                try {
                    ldapUserProvisioningService.deleteAdUser(username);
                } catch (Exception cleanupEx) {
                    log.error("CRITICAL: AD user '{}' exists but DB save failed AND AD rollback also failed. " +
                            "Manual AD cleanup required immediately.", username, cleanupEx);
                }
            }
            throw dbException;
        }
    }

    @Transactional
    public AppUser updateUser(Long userId, String roleText, String agencyName, String rawPassword,
                             String fullName, String email, String phone) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (roleText != null && !roleText.isBlank()) {
            user.setRole(UserRole.valueOf(roleText.toUpperCase()));
        }
        if (agencyName != null) {
            user.setAgencyName(agencyName);
        }
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        if (fullName != null) {
            user.setFullName(fullName);
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (phone != null) {
            user.setPhone(phone);
        }

        AppUser saved = userRepository.save(user);
        syncRoleProfile(saved, saved.getFullName(), saved.getPhone());
        return saved;
    }

    @Transactional
    public void deleteUser(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == UserRole.FIELD_PERSON) {
            fieldPersonRepository.findByUserId(userId).ifPresent(fieldPersonRepository::delete);
        }

        userRepository.delete(user);
    }

    @Transactional
    protected void syncRoleProfile(AppUser user, String name, String phone) {
        if (user == null) {
            return;
        }

        if (user.getRole() != UserRole.FIELD_PERSON) {
            return;
        }

        FieldPerson existing = fieldPersonRepository.findByUserId(user.getId()).orElse(null);
        if (existing == null) {
            FieldPerson fieldPerson = FieldPerson.builder()
                    .name(name != null && !name.isBlank() ? name : user.getFullName() != null ? user.getFullName() : user.getUsername())
                    .role("Field Person")
                    .phone(phone)
                    .active(true)
                    .user(user)
                    .build();
            fieldPersonRepository.save(fieldPerson);
            return;
        }

        if (name != null && !name.isBlank()) {
            existing.setName(name);
        }
        if (phone != null) {
            existing.setPhone(phone);
        }
        existing.setUser(user);
        existing.setActive(true);
        fieldPersonRepository.save(existing);
    }
}