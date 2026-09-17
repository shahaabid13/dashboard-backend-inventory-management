package com.inventory.msp.incident.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateUserRequest {

    private String role;
    private String agencyName;
    private String password;

    // Editable contact fields
    private String fullName;
    private String email;
    private String phone;
}
