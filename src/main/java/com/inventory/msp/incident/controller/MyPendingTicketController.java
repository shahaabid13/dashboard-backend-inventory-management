package com.inventory.msp.incident.controller;

import com.inventory.msp.exception.NotFoundException;
import com.inventory.msp.incident.dto.MyPendingTicketDTO;
import com.inventory.msp.incident.service.TicketFlagService;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cims/my-pending-tickets")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MyPendingTicketController {

    private final TicketFlagService ticketFlagService;
    private final UserRepository userRepository;

    @GetMapping
    public List<MyPendingTicketDTO> getMyPendingTickets(Authentication authentication) {
        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        return ticketFlagService.getMyPendingTickets(user.getId());
    }
}
