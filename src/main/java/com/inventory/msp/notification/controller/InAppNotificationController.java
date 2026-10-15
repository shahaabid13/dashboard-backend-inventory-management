package com.inventory.msp.notification.controller;

import com.inventory.msp.notification.dto.InAppNotificationDTO;
import com.inventory.msp.exception.NotFoundException;
import com.inventory.msp.incident.service.NotificationService;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InAppNotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping({"", "/my"})
    public List<InAppNotificationDTO> getMyNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long sinceId) {
        return notificationService.getInAppNotifications(
                currentUser(authentication).getUsername(), unreadOnly, type, sinceId);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(Authentication authentication) {
        return Map.of("unreadCount",
                notificationService.getUnreadNotificationCount(currentUser(authentication).getUsername()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, Authentication authentication) {
        if (!notificationService.markNotificationRead(currentUser(authentication).getUsername(), id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead(Authentication authentication) {
        return Map.of("markedRead",
                notificationService.markAllNotificationsRead(currentUser(authentication).getUsername()));
    }

    private AppUser currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
