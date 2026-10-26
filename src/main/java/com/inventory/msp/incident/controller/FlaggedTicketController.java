package com.inventory.msp.incident.controller;

import com.inventory.msp.exception.NotFoundException;
import com.inventory.msp.incident.dto.FlaggedTicketDTO;
import com.inventory.msp.incident.service.TicketFlagService;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.UserRole;
import com.inventory.msp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/cims/flagged-tickets")
@RequiredArgsConstructor
public class FlaggedTicketController {

    private static final Set<UserRole> GLOBAL_ROLES = Set.of(
            UserRole.ADMIN, UserRole.COORDINATOR, UserRole.REVIEWER);

    private final TicketFlagService ticketFlagService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<FlaggedTicketDTO> getFlaggedTickets(Authentication authentication) {
        AppUser user = currentUser(authentication);
        Long userId = GLOBAL_ROLES.contains(user.getRole()) ? null : user.getId();
        return ticketFlagService.getFlaggedTickets(userId);
    }

    @PostMapping("/{id}/remind")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public ResponseEntity<FlaggedTicketDTO> remind(@PathVariable Long id) {
        FlaggedTicketDTO response = ticketFlagService.sendManualReminder(id);
        return response.getRemindersSent() > 0
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private AppUser currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
