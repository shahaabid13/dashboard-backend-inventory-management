package com.inventory.msp.incident.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldPersonRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Role is required")
    private String role;

    private String phone;

    private Boolean active = true;

    /**
     * Optional. Links this FieldPerson profile to an existing AppUser login account
     * (must have role FIELD_PERSON). Required for the person to appear in "assignable"
     * lists, log in and see their queue, etc.
     */
    private Long userId;
}