package com.inventory.msp.controller;

import com.inventory.msp.incident.model.Ticket;
import com.inventory.msp.incident.model.TicketHistory;
import com.inventory.msp.incident.repository.TicketHistoryRepository;
import com.inventory.msp.incident.repository.TicketRepository;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.Attachment;
import com.inventory.msp.repository.AttachmentRepository;
import com.inventory.msp.repository.UserRepository;
import com.inventory.msp.services.FileStorageService;
import com.inventory.msp.task.model.Task;
import com.inventory.msp.task.model.TaskHistory;
import com.inventory.msp.task.repository.TaskHistoryRepository;
import com.inventory.msp.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;

    @Value("${app.upload.max-attachments-per-entity:${APP_UPLOAD_MAX_ATTACHMENTS_PER_ENTITY:5}}")
    private int maxAttachmentsPerEntity;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttachmentResponse> createAttachment(
            @RequestPart("file") MultipartFile file,
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") Long entityId,
            Authentication authentication) {
        AppUser currentUser = getCurrentUser(authentication);
        String normalizedEntityType = normalizeEntityType(entityType);
        ensureCanAccessEntity(normalizedEntityType, entityId, currentUser);

        long count = attachmentRepository.countByEntityTypeAndEntityId(normalizedEntityType, entityId);
        if (count >= maxAttachmentsPerEntity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum of 5 attachments allowed for this item");
        }

        FileStorageService.StoredImage storedImage = fileStorageService.store(file);

        Attachment attachment = Attachment.builder()
                .entityType(normalizedEntityType)
                .entityId(entityId)
                .originalFileName(file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename())
                .storedFileName(storedImage.storedFileName())
                .contentType(storedImage.contentType())
                .fileSizeBytes(storedImage.fileSizeBytes())
                .uploadedByUser(currentUser)
                .build();

        Attachment saved = attachmentRepository.save(attachment);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AttachmentResponse>> getAttachments(
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") Long entityId,
            Authentication authentication) {
        String normalizedEntityType = normalizeEntityType(entityType);
        AppUser currentUser = getCurrentUser(authentication);
        ensureCanAccessEntity(normalizedEntityType, entityId, currentUser);

        List<Attachment> attachments = attachmentRepository.findByEntityTypeAndEntityId(normalizedEntityType, entityId);
        return ResponseEntity.ok(attachments.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}/file")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadAttachmentFile(@PathVariable Long id, Authentication authentication) {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        AppUser currentUser = getCurrentUser(authentication);
        ensureCanAccessEntity(attachment.getEntityType(), attachment.getEntityId(), currentUser);

        Resource resource = fileStorageService.loadAsResource(attachment.getStoredFileName());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long id, Authentication authentication) {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));

        AppUser currentUser = getCurrentUser(authentication);
        boolean isAdmin = currentUser.getRole() != null && currentUser.getRole().name().equals("ADMIN");
        boolean isUploader = attachment.getUploadedByUser() != null && attachment.getUploadedByUser().getId().equals(currentUser.getId());
        if (!isAdmin && !isUploader) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to delete this attachment");
        }

        fileStorageService.deleteFile(attachment.getStoredFileName());
        attachmentRepository.delete(attachment);
        return ResponseEntity.noContent().build();
    }

    private AppUser getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private String normalizeEntityType(String entityType) {
        if (entityType == null || entityType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entityType is required");
        }
        String normalized = entityType.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("TASK") && !normalized.equals("TICKET")
                && !normalized.equals("TASK_ACTION") && !normalized.equals("TICKET_ACTION")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid entityType: " + entityType);
        }
        return normalized;
    }

    private void ensureCanAccessEntity(String entityType, Long entityId, AppUser currentUser) {
        if (entityId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entityId is required");
        }

        if (entityType.equals("TASK")) {
            Task task = taskRepository.findById(entityId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
            if (!canViewTask(task, currentUser)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this task attachment");
            }
            return;
        }

        if (entityType.equals("TASK_ACTION")) {
            TaskHistory history = taskHistoryRepository.findById(entityId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task action not found"));
            if (!canViewTask(history.getTask(), currentUser)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this task action attachment");
            }
            return;
        }

        if (entityType.equals("TICKET")) {
            Ticket ticket = ticketRepository.findById(entityId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
            if (!canViewTicket(ticket, currentUser)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this ticket attachment");
            }
            return;
        }

        TicketHistory ticketHistory = ticketHistoryRepository.findById(entityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket action not found"));
        Ticket ticket = ticketHistory.getTicket();
        if (ticket == null || !canViewTicket(ticket, currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this ticket action attachment");
        }
    }

    private boolean canViewTicket(Ticket ticket, AppUser user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        if (user.getRole().name().equals("ADMIN")) {
            return true;
        }
        if (ticket.getRaisedByUser() != null && user.getId().equals(ticket.getRaisedByUser().getId())) {
            return true;
        }
        if (ticket.getCoordinator() != null && user.getId().equals(ticket.getCoordinator().getId())) {
            return true;
        }
        if (ticket.getReviewer() != null && user.getId().equals(ticket.getReviewer().getId())) {
            return true;
        }
        return ticket.getFieldPerson() != null
                && ticket.getFieldPerson().getUser() != null
                && user.getId().equals(ticket.getFieldPerson().getUser().getId());
    }

    private boolean canViewTask(Task task, AppUser user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        if (user.getRole().name().equals("ADMIN")
                || user.getRole().name().equals("REVIEWER")
                || user.getRole().name().equals("SUPPORT_ENGINEER")) {
            return true;
        }
        return task.getAssignedToUser() != null && user.getId().equals(task.getAssignedToUser().getId());
    }

    private AttachmentResponse toResponse(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getFileSizeBytes(),
                attachment.getUploadedAt(),
                "/api/attachments/" + attachment.getId() + "/file");
    }

    public record AttachmentResponse(
            Long id,
            String originalFileName,
            String contentType,
            Long fileSizeBytes,
            LocalDateTime uploadedAt,
            String downloadUrl) {
    }
}
