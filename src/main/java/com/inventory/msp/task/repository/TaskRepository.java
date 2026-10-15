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

    @Query(value = """
            SELECT COALESCE(SUM(CASE WHEN status = 'OPEN' THEN 1 ELSE 0 END), 0) AS pendingCount,
                   COALESCE(SUM(CASE WHEN status = 'OPEN' AND created_at < :overdueBefore
                                     THEN 1 ELSE 0 END), 0) AS overdueCount,
                   COALESCE(SUM(CASE WHEN status = 'RESOLVED'
                                         AND action_taken_at >= :weekStart
                                         AND action_taken_at < :now
                                     THEN 1 ELSE 0 END), 0) AS completedThisWeek
            FROM tasks
            WHERE (CAST(:userId AS SIGNED) IS NULL OR assigned_to_user_id = CAST(:userId AS SIGNED))
            """, nativeQuery = true)
    TaskDashboardAggregate getDashboardAggregate(
            @Param("overdueBefore") LocalDateTime overdueBefore,
            @Param("weekStart") LocalDateTime weekStart,
            @Param("now") LocalDateTime now,
            @Param("userId") Long userId);

    @Query("""
            SELECT t.status AS status, COUNT(t.id) AS taskCount
            FROM Task t
            WHERE :userId IS NULL OR t.assignedToUser.id = :userId
            GROUP BY t.status
            """)
    List<TaskStatusCount> countTasksByStatus(@Param("userId") Long userId);

    @Query(value = """
            SELECT u.username AS userName,
                   SUM(CASE WHEN t.status = 'OPEN' THEN 1 ELSE 0 END) AS pending,
                   SUM(CASE WHEN t.status = 'OPEN' AND t.created_at < :overdueBefore
                            THEN 1 ELSE 0 END) AS overdue
            FROM tasks t
            JOIN app_user u ON u.id = t.assigned_to_user_id
            WHERE (CAST(:userId AS SIGNED) IS NULL OR t.assigned_to_user_id = CAST(:userId AS SIGNED))
            GROUP BY u.id, u.username
            ORDER BY pending DESC
            """, nativeQuery = true)
    List<TaskAssigneeCounts> countPendingTasksByAssignee(
            @Param("overdueBefore") LocalDateTime overdueBefore,
            @Param("userId") Long userId);

    interface TaskDashboardAggregate {
        Long getPendingCount();
        Long getOverdueCount();
        Long getCompletedThisWeek();
    }

    interface TaskStatusCount {
        TaskStatus getStatus();
        Long getTaskCount();
    }

    interface TaskAssigneeCounts {
        String getUserName();
        Long getPending();
        Long getOverdue();
    }
}
