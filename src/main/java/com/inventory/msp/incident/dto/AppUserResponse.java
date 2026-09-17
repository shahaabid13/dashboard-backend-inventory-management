package com.inventory.msp.incident.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUserResponse {

    private Long id;
    private String username;
    private String role;
    private String agencyName;

    private String fullName;
    private String email;
    private String phone;
}

