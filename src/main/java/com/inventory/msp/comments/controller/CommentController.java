package com.inventory.msp.comments.controller;

import com.inventory.msp.comments.dto.CommentDto;
import com.inventory.msp.comments.dto.CommentPermissionDto;
import com.inventory.msp.comments.dto.CreateCommentRequest;
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
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class CommentController {
    private final CommentService commentService;
    private final UserRepository userRepository;

    @GetMapping
    public List<CommentDto> getComments(
            @RequestParam("entityType") EntityType entityType,
            @RequestParam("entityId") @Positive Long entityId) {
        return commentService.getComments(entityType, entityId);
    }

    @GetMapping("/permission")
    public CommentPermissionDto getCommentPermission(
            @RequestParam("entityType") EntityType entityType,
            @RequestParam("entityId") @Positive Long entityId,
            Authentication authentication) {
        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        return new CommentPermissionDto(commentService.canComment(entityType, entityId, user));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@Valid @RequestBody CreateCommentRequest request) {
        return commentService.createComment(request);
    }

}
