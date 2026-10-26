package com.inventory.msp.task.repository;

import com.inventory.msp.task.model.Task;
import com.inventory.msp.task.model.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {

    // Get history for a task, ordered by most recent first
    List<TaskHistory> findByTaskOrderByChangedAtDesc(Task task);
}
