package com.inventory.msp.incident.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "Role is required")
    private String role;

    @JsonAlias("agency_name")
    private String agencyName;

    // For FIELD_PERSON role
    private String name;
    @JsonAlias({"mobile", "phoneNumber", "phone_number"})
    private String phone;

    // New fields to store on AppUser
    @JsonAlias("full_name")
    private String fullName;
    private String email;
}
