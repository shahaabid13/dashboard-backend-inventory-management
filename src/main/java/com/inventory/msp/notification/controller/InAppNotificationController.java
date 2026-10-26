package com.inventory.msp.notification.controller;

import com.inventory.msp.comments.dto.CommentNotificationDto;
import com.inventory.msp.comments.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InAppNotificationController {
    private final CommentService commentService;

    @GetMapping
    public List<CommentNotificationDto> getMyNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return commentService.getNotifications(unreadOnly);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount() {
        return Map.of("unreadCount", commentService.getUnreadNotificationCount());
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        commentService.markNotificationRead(id);
        return ResponseEntity.noContent().build();
    }
}
