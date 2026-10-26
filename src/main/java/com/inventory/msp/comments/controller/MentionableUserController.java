package com.inventory.msp.comments.controller;

import com.inventory.msp.comments.dto.MentionableUserDto;
import com.inventory.msp.comments.model.EntityType;
import com.inventory.msp.comments.service.CommentService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/mentionable")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class MentionableUserController {
    private final CommentService commentService;

    @GetMapping
    public List<MentionableUserDto> searchMentionableUsers(
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam("entityType") EntityType entityType,
            @RequestParam("entityId") @Positive Long entityId) {
        return commentService.searchMentionableUsers(q, entityType, entityId);
    }
}
