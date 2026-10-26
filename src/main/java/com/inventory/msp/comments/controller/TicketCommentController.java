package com.inventory.msp.comments.controller;

import com.inventory.msp.comments.dto.CommentDto;
import com.inventory.msp.comments.dto.CommentPermissionDto;
import com.inventory.msp.comments.dto.TicketCommentRequest;
import com.inventory.msp.comments.model.EntityType;
import com.inventory.msp.comments.service.CommentService;
import com.inventory.msp.exception.NotFoundException;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class TicketCommentController {
    private final CommentService commentService;
    private final UserRepository userRepository;

    @GetMapping
    public List<CommentDto> getTicketComments(@PathVariable @Positive Long ticketId) {
        return commentService.getComments(EntityType.TICKET, ticketId);
    }

    @GetMapping("/permission")
    public CommentPermissionDto getCommentPermission(
            @PathVariable @Positive Long ticketId,
            Authentication authentication) {
        return new CommentPermissionDto(commentService.canComment(ticketId, currentUser(authentication)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createTicketComment(
            @PathVariable @Positive Long ticketId,
            @Valid @RequestBody TicketCommentRequest request,
            Authentication authentication) {
        return commentService.createTicketComment(
                ticketId, request.body(), request.mentionedUserIds(), currentUser(authentication));
    }

    private AppUser currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }
}
