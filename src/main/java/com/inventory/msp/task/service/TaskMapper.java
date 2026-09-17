package com.inventory.msp.task.service;

import com.inventory.msp.task.dto.*;
import com.inventory.msp.task.model.Task;
import com.inventory.msp.task.model.TaskHistory;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public TaskResponse toTaskResponse(Task task) {
        if (task == null) {
            return null;
        }

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus().name())
                .assignedToUserId(task.getAssignedToUser() != null ? task.getAssignedToUser().getId() : null)
                .assignedToUsername(task.getAssignedToUser() != null ? task.getAssignedToUser().getUsername() : null)
                .assignedToFullName(task.getAssignedToUser() != null ? task.getAssignedToUser().getFullName() : null)
                .assignedByUserId(task.getAssignedByUser() != null ? task.getAssignedByUser().getId() : null)
                .assignedByUsername(task.getAssignedByUser() != null ? task.getAssignedByUser().getUsername() : null)
                .assignedByFullName(task.getAssignedByUser() != null ? task.getAssignedByUser().getFullName() : null)
                .actionSummary(task.getActionSummary())
                .actionTakenAt(task.getActionTakenAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    public TaskDetailResponse toTaskDetailResponse(Task task) {
        if (task == null) {
            return null;
        }

        return TaskDetailResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus().name())
                .assignedToUserId(task.getAssignedToUser() != null ? task.getAssignedToUser().getId() : null)
                .assignedToUsername(task.getAssignedToUser() != null ? task.getAssignedToUser().getUsername() : null)
                .assignedToFullName(task.getAssignedToUser() != null ? task.getAssignedToUser().getFullName() : null)
                .assignedByUserId(task.getAssignedByUser() != null ? task.getAssignedByUser().getId() : null)
                .assignedByUsername(task.getAssignedByUser() != null ? task.getAssignedByUser().getUsername() : null)
                .assignedByFullName(task.getAssignedByUser() != null ? task.getAssignedByUser().getFullName() : null)
                .actionSummary(task.getActionSummary())
                .actionTakenAt(task.getActionTakenAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    public TaskHistoryResponse toTaskHistoryResponse(TaskHistory history) {
        if (history == null) {
            return null;
        }

        return TaskHistoryResponse.builder()
                .id(history.getId())
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .notes(history.getNotes())
                .changedByUserId(history.getChangedByUser() != null ? history.getChangedByUser().getId() : null)
                .changedByUsername(history.getChangedByUser() != null ? history.getChangedByUser().getUsername() : null)
                .changedByFullName(history.getChangedByUser() != null ? history.getChangedByUser().getFullName() : null)
                .changedAt(history.getChangedAt())
                .build();
    }
}
