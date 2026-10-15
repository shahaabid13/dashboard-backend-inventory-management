package com.inventory.msp.incident.controller;

import com.inventory.msp.exception.NotFoundException;
import com.inventory.msp.incident.dto.*;
import com.inventory.msp.incident.service.CimsDashboardService;
import com.inventory.msp.incident.repository.FieldPersonRepository;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.UserRole;
import com.inventory.msp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/cims/dashboard")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CimsDashboardController {

    private static final Set<UserRole> GLOBAL_ROLES = Set.of(
            UserRole.ADMIN, UserRole.COORDINATOR, UserRole.REVIEWER);

    private final CimsDashboardService dashboardService;
    private final UserRepository userRepository;
    private final FieldPersonRepository fieldPersonRepository;

    @GetMapping("/summary")
    public CimsDashboardSummaryDTO getSummary(Authentication authentication) {
        AppUser user = currentUser(authentication);
        return dashboardService.getSummary(
                dashboardService.scopedTaskUserId(user), scopedFieldPersonId(user));
    }

    @GetMapping("/tickets-by-assignee")
    public List<DashboardCountDTO> getTicketsByAssignee(Authentication authentication) {
        return dashboardService.getTicketsByAssignee(scopedFieldPersonId(currentUser(authentication)));
    }

    @GetMapping("/tickets-by-status")
    public List<TicketStatusCountDTO> getTicketsByStatus(Authentication authentication) {
        return dashboardService.getTicketsByStatus(scopedFieldPersonId(currentUser(authentication)));
    }

    @GetMapping("/tickets-by-incident-type")
    public List<TicketIncidentTypeCountDTO> getTicketsByIncidentType(Authentication authentication) {
        return dashboardService.getTicketsByIncidentType(scopedFieldPersonId(currentUser(authentication)));
    }

    @GetMapping("/tickets-ageing-buckets")
    public List<TicketAgeingBucketDTO> getTicketAgeingBuckets(Authentication authentication) {
        return dashboardService.getTicketAgeingBuckets(scopedFieldPersonId(currentUser(authentication)));
    }

    @GetMapping("/tickets-trend")
    public List<TicketTrendDTO> getTicketsTrend(
            @RequestParam(defaultValue = "30") int days,
            Authentication authentication) {
        return dashboardService.getTicketsTrend(days, scopedFieldPersonId(currentUser(authentication)));
    }

    @GetMapping("/tasks-by-status")
    public List<TaskStatusCountDTO> getTasksByStatus(Authentication authentication) {
        return dashboardService.getTasksByStatus(
                dashboardService.scopedTaskUserId(currentUser(authentication)));
    }

    @GetMapping("/tasks-by-assignee")
    public List<TaskAssigneeCountDTO> getTasksByAssignee(Authentication authentication) {
        return dashboardService.getTasksByAssignee(
                dashboardService.scopedTaskUserId(currentUser(authentication)));
    }

    private AppUser currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private Long scopedFieldPersonId(AppUser user) {
        if (GLOBAL_ROLES.contains(user.getRole())) {
            return null;
        }
        return fieldPersonRepository.findByUserId(user.getId())
                .map(fieldPerson -> fieldPerson.getId())
                .orElse(-1L);
    }
}
