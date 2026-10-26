package com.inventory.msp.task.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskHistoryResponse {

    private Long id;
    private String fromStatus;
    private String toStatus;
    private String notes;
    
    private Long changedByUserId;
    private String changedByUsername;
    private String changedByFullName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime changedAt;
}
