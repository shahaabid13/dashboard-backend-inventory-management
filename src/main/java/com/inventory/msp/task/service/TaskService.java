package com.inventory.msp.task.service;

import com.inventory.msp.exception.NotFoundException;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.repository.UserRepository;
import com.inventory.msp.task.dto.*;
import com.inventory.msp.task.model.Task;
import com.inventory.msp.task.model.TaskHistory;
import com.inventory.msp.task.model.TaskStatus;
import com.inventory.msp.task.repository.TaskHistoryRepository;
import com.inventory.msp.task.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    public TaskService(TaskRepository taskRepository, TaskHistoryRepository taskHistoryRepository,
                       UserRepository userRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskHistoryRepository = taskHistoryRepository;
        this.userRepository = userRepository;
        this.taskMapper = taskMapper;
    }

    /**
     * Create a new task with OPEN status, assigned to a user, by the requesting user.
     * Records an initial task_history entry ("Task created").
     */
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, Long assignedByUserId) {
        AppUser assignedToUser = userRepository.findById(request.getAssignedToUserId())
                .orElseThrow(() -> new NotFoundException(
                        "User not found with ID: " + request.getAssignedToUserId()));

        AppUser assignedByUser = userRepository.findById(assignedByUserId)
                .orElseThrow(() -> new NotFoundException(
                        "User not found with ID: " + assignedByUserId));

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .assignedToUser(assignedToUser)
                .assignedByUser(assignedByUser)
                .status(TaskStatus.OPEN)
                .build();

        task = taskRepository.save(task);

        // Record initial history entry
        TaskHistory history = TaskHistory.builder()
                .task(task)
                .fromStatus(null)
                .toStatus(TaskStatus.OPEN.name())
                .notes("Task created")
                .changedByUser(assignedByUser)
                .build();

        taskHistoryRepository.save(history);

        log.info("Task created with ID {}, title '{}', assigned to user {} by user {}",
                task.getId(), task.getTitle(), assignedToUser.getUsername(), assignedByUser.getUsername());

        return taskMapper.toTaskResponse(task);
    }

    /**
     * Get all tasks assigned to a specific user with OPEN status (their pending queue).
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getMyTasks(Long userId, Pageable pageable) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        Page<Task> tasks = taskRepository.findByAssignedToUserAndStatus(user, TaskStatus.OPEN, pageable);
        return tasks.map(taskMapper::toTaskResponse);
    }

    /**
     * Get all tasks ever assigned to a specific user, regardless of status.
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getMyTaskHistory(Long userId, Pageable pageable) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        Page<Task> tasks = taskRepository.findByAssignedToUserOrderByCreatedAtDesc(user, pageable);
        return tasks.map(taskMapper::toTaskResponse);
    }

    /**
     * Get all tasks with optional filters (for Admin/Reviewer/Support Engineer dashboard view).
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasks(Long assignedToUserId, Long assignedByUserId, String status, 
                                          LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        TaskStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = TaskStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }
        }

        Page<Task> tasks = taskRepository.findWithFilters(assignedToUserId, assignedByUserId, statusEnum,
                fromDate, toDate, pageable);
        return tasks.map(taskMapper::toTaskResponse);
    }

    /**
     * Take action on a task: transition OPEN or HOLD → RESOLVED/HOLD/REJECTED (or HOLD → RESOLVED/REJECTED).
     * Only the assigned user or ADMIN can act on a task.
     * Records a task_history entry with the action taken.
     */
    @Transactional
    public TaskResponse takeAction(Long taskId, TaskActionRequest request, Long actingUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found with ID: " + taskId));

        AppUser actingUser = userRepository.findById(actingUserId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + actingUserId));

        // Ownership validation: only assigned user or ADMIN can act
        boolean isAssignee = task.getAssignedToUser().getId().equals(actingUserId);
        boolean isAdmin = actingUser.getRole().name().equals("ADMIN");

        if (!isAssignee && !isAdmin) {
            throw new IllegalArgumentException(
                    "You are not authorized to take action on this task (not assignee or admin)");
        }

        // Status validation: can only act on OPEN or HOLD tasks (not RESOLVED/REJECTED)
        if (!task.getStatus().equals(TaskStatus.OPEN) && !task.getStatus().equals(TaskStatus.HOLD)) {
            throw new IllegalArgumentException(
                    "Cannot take action on a task with status: " + task.getStatus().name());
        }

        // Parse the requested new status
        TaskStatus newStatus;
        try {
            newStatus = TaskStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid status: " + request.getStatus() + ". Allowed: RESOLVED, HOLD, REJECTED");
        }

        // If current status is OPEN, allow transition to RESOLVED, HOLD, or REJECTED
        // If current status is HOLD, allow transition to RESOLVED or REJECTED (but NOT back to OPEN)
        if (task.getStatus().equals(TaskStatus.OPEN)) {
            if (!newStatus.equals(TaskStatus.RESOLVED) && !newStatus.equals(TaskStatus.HOLD) 
                    && !newStatus.equals(TaskStatus.REJECTED)) {
                throw new IllegalArgumentException(
                        "From OPEN status, can only transition to RESOLVED, HOLD, or REJECTED");
            }
        } else if (task.getStatus().equals(TaskStatus.HOLD)) {
            if (!newStatus.equals(TaskStatus.RESOLVED) && !newStatus.equals(TaskStatus.REJECTED)) {
                throw new IllegalArgumentException(
                        "From HOLD status, can only transition to RESOLVED or REJECTED");
            }
        }

        TaskStatus oldStatus = task.getStatus();
        task.setStatus(newStatus);
        task.setActionSummary(request.getSummary());
        task.setActionTakenAt(LocalDateTime.now());

        task = taskRepository.save(task);

        // Record history entry
        TaskHistory history = TaskHistory.builder()
                .task(task)
                .fromStatus(oldStatus.name())
                .toStatus(newStatus.name())
                .notes(request.getSummary())
                .changedByUser(actingUser)
                .build();

        taskHistoryRepository.save(history);

        log.info("Task {} transitioned from {} to {} by user {}", taskId, oldStatus.name(), newStatus.name(),
                actingUser.getUsername());

        return taskMapper.toTaskResponse(task);
    }

    /**
     * Get full history list for a task (ordered by most recent first).
     */
    @Transactional(readOnly = true)
    public List<TaskHistoryResponse> getTaskHistory(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found with ID: " + taskId));

        List<TaskHistory> history = taskHistoryRepository.findByTaskOrderByChangedAtDesc(task);
        return history.stream().map(taskMapper::toTaskHistoryResponse).collect(Collectors.toList());
    }

    /**
     * Get task detail with full history included.
     */
    @Transactional(readOnly = true)
    public TaskDetailResponse getTaskDetail(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found with ID: " + taskId));

        TaskDetailResponse response = taskMapper.toTaskDetailResponse(task);
        List<TaskHistoryResponse> history = getTaskHistory(taskId);
        response.setHistory(history);
        return response;
    }
}
