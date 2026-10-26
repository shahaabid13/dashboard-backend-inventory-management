package com.inventory.msp.comments.dto;

public record MentionableUserDto(
        Long id,
        String username,
        String name,
        String fullName,
        String displayName,
        String role) {
}
