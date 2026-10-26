package com.inventory.msp.comments.dto;

import com.inventory.msp.comments.model.EntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateCommentRequest {
    @NotNull
    private EntityType entityType;

    @NotNull
    @Positive
    private Long entityId;

    @NotBlank
    @Size(max = 2000)
    private String body;

    private List<@NotNull @Positive Long> mentionedUserIds = List.of();
}
