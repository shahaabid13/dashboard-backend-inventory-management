package com.inventory.msp.task.repository;

import com.inventory.msp.task.model.Task;
import com.inventory.msp.task.model.TaskStatus;
import com.inventory.msp.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // Tasks assigned to a specific user with specific status (typically OPEN)
    Page<Task> findByAssignedToUserAndStatus(AppUser assignedToUser, TaskStatus status, Pageable pageable);

    // All tasks ever assigned to a specific user (regardless of status)
    Page<Task> findByAssignedToUserOrderByCreatedAtDesc(AppUser assignedToUser, Pageable pageable);

    // All tasks assigned by a specific user
    Page<Task> findByAssignedByUserOrderByCreatedAtDesc(AppUser assignedByUser, Pageable pageable);

    // Count tasks by status
    long countByStatus(TaskStatus status);

    // Advanced filtering for Admin dashboard
    @Query("SELECT t FROM Task t WHERE " +
           "(:assignedToUserId IS NULL OR t.assignedToUser.id = :assignedToUserId) AND " +
           "(:assignedByUserId IS NULL OR t.assignedByUser.id = :assignedByUserId) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:fromDate IS NULL OR t.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR t.createdAt <= :toDate)")
    Page<Task> findWithFilters(
            @Param("assignedToUserId") Long assignedToUserId,
            @Param("assignedByUserId") Long assignedByUserId,
            @Param("status") TaskStatus status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}
