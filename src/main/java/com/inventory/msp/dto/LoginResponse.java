package com.inventory.msp.dto;


import com.inventory.msp.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private UserRole role;
}
