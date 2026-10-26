package com.inventory.msp.comments.service;

import com.inventory.msp.comments.model.EntityType;
import com.inventory.msp.comments.repository.CommentMentionRepository;
import com.inventory.msp.incident.model.Ticket;
import com.inventory.msp.incident.repository.TicketRepository;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.UserRole;
import com.inventory.msp.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommentPermissionService {
    private static final Set<String> TICKET_COMMENTER_USERNAMES = Set.of("appadmin", "wasiya");

    private final TaskRepository taskRepository;
    private final TicketRepository ticketRepository;
    private final CommentMentionRepository commentMentionRepository;

    public boolean canComment(EntityType entityType, Long entityId, AppUser user) {
        if (entityType == EntityType.TASK) {
            taskRepository.findById(entityId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        } else {
            ticketRepository.findById(entityId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        }
        if (user.getRole() == UserRole.ADMIN
                || (entityType == EntityType.TICKET
                && user.getUsername() != null
                && TICKET_COMMENTER_USERNAMES.stream()
                .anyMatch(username -> username.equalsIgnoreCase(user.getUsername())))) {
            return true;
        }
        return user.getId() != null
                && commentMentionRepository.countForEntityAndMentionedUser(
                        entityType.name(), entityId, user.getId()) > 0;
    }

    public void requireRead(EntityType entityType, Long entityId, AppUser user) {
        if (entityType == EntityType.TASK) {
            taskRepository.findById(entityId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        } else {
            ticketRepository.findById(entityId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        }
    }

    public void requireWrite(EntityType entityType, Long entityId, AppUser user) {
        if (!canComment(entityType, entityId, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can comment once you're mentioned");
        }
    }

}
