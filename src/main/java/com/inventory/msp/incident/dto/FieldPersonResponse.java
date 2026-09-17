package com.inventory.msp.incident.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldPersonResponse {

    private Long id;
    private String name;
    private String role;
    private String phone;
    private Boolean active;
    private LocalDateTime createdAt;
}

