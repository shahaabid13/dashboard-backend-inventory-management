package com.inventory.msp.comments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TicketCommentRequest(
        @NotBlank
        @Size(max = 2000)
        String body,
        List<@NotNull @Positive Long> mentionedUserIds) {
}
