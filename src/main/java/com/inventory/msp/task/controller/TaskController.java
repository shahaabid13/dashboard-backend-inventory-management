package com.inventory.msp.task.controller;

import com.inventory.msp.exception.NotFoundException;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.repository.UserRepository;
import com.inventory.msp.task.dto.*;
import com.inventory.msp.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/tasks")
@Slf4j
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;

    /**
     * POST /api/tasks — Create a new task.
     * Allowed: ADMIN, REVIEWER
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request,
                                                    Authentication authentication) {
        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        TaskResponse response = taskService.createTask(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/tasks/my — Get assignee's own pending tasks (OPEN status).
     * Allowed: Any authenticated user
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<TaskResponse>> getMyTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Pageable pageable = PageRequest.of(page, size);
        Page<TaskResponse> tasks = taskService.getMyTasks(user.getId(), pageable);
        return ResponseEntity.ok(tasks);
    }

    /**
     * GET /api/tasks/my-history — Get assignee's own full task history (all statuses).
     * Allowed: Any authenticated user
     */
    @GetMapping("/my-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<TaskResponse>> getMyTaskHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Pageable pageable = PageRequest.of(page, size);
        Page<TaskResponse> tasks = taskService.getMyTaskHistory(user.getId(), pageable);
        return ResponseEntity.ok(tasks);
    }

    /**
     * PUT /api/tasks/{id}/action — Take action on a task (transition status).
     * Allowed: Any authenticated user (service validates ownership/admin)
     */
    @PutMapping("/{id}/action")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponse> takeAction(@PathVariable Long id,
                                                    @Valid @RequestBody TaskActionRequest request,
                                                    Authentication authentication) {
        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        TaskResponse response = taskService.takeAction(id, request, user.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/tasks — Full task list with optional filters (Admin/Reviewer/Support Engineer view).
     * Allowed: ADMIN, REVIEWER, SUPPORT_ENGINEER
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'SUPPORT_ENGINEER')")
    public ResponseEntity<Page<TaskResponse>> getAllTasks(
            @RequestParam(required = false) Long assignedToUserId,
            @RequestParam(required = false) Long assignedByUserId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TaskResponse> tasks = taskService.getAllTasks(assignedToUserId, assignedByUserId, status,
                fromDate, toDate, pageable);
        return ResponseEntity.ok(tasks);
    }

    /**
     * GET /api/tasks/{id} — Get task detail with full history.
     * Allowed: ADMIN, REVIEWER, SUPPORT_ENGINEER, or the assigned user themselves
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskDetailResponse> getTaskDetail(@PathVariable Long id,
                                                            Authentication authentication) {
        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        
        TaskDetailResponse response = taskService.getTaskDetail(id);
        
        // Authorization: Admin/Reviewer/Support Engineer can view all,
        // others can only view if assigned to them
        boolean canView = user.getRole().name().equals("ADMIN") ||
                user.getRole().name().equals("REVIEWER") ||
                user.getRole().name().equals("SUPPORT_ENGINEER") ||
                (response.getAssignedToUserId() != null && 
                 user.getId().equals(response.getAssignedToUserId()));
        
        if (!canView) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(response);
    }
}
