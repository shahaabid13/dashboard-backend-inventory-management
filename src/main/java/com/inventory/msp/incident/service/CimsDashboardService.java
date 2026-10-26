package com.inventory.msp.incident.service;

import com.inventory.msp.incident.dto.*;
import com.inventory.msp.incident.model.TicketQueueRules;
import com.inventory.msp.incident.model.TicketStatus;
import com.inventory.msp.incident.repository.TicketRepository;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.task.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CimsDashboardService {

    private static final List<String> AGE_BUCKETS = List.of("0-1d", "1-3d", "3-7d", "7d+");
    private final TicketRepository ticketRepository;
    private final TaskRepository taskRepository;

    public CimsDashboardService(TicketRepository ticketRepository, TaskRepository taskRepository) {
        this.ticketRepository = ticketRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public CimsDashboardSummaryDTO getSummary(Long userId, Long fieldPersonId) {
        LocalDateTime now = LocalDateTime.now(TicketQueueRules.REPORT_ZONE);
        LocalDate today = now.toLocalDate();
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime overdueBefore = now.minusHours(24);
        TicketRepository.TicketDashboardAggregate tickets =
                ticketRepository.getDashboardAggregate(
                        today, TicketQueueRules.QUEUE_STATUS_NAMES, fieldPersonId);
        TaskRepository.TaskDashboardAggregate tasks =
                taskRepository.getDashboardAggregate(overdueBefore, weekStart, now, userId);
        return CimsDashboardSummaryDTO.builder()
                .totalInQueue(value(tickets == null ? null : tickets.getTotalInQueue()))
                .flaggedCount(value(tickets == null ? null : tickets.getFlaggedCount()))
                .avgAgeDays(tickets == null || tickets.getAvgAgeDays() == null ? 0 : tickets.getAvgAgeDays())
                .oldestAgeDays(value(tickets == null ? null : tickets.getOldestAgeDays()))
                .tasksPending(value(tasks == null ? null : tasks.getPendingCount()))
                .tasksOverdue(value(tasks == null ? null : tasks.getOverdueCount()))
                .tasksCompletedThisWeek(value(tasks == null ? null : tasks.getCompletedThisWeek()))
                .build();
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public List<DashboardCountDTO> getTicketsByAssignee(Long fieldPersonId) {
        return ticketRepository.countOpenTicketsByAssignee(TicketQueueRules.QUEUE_STATUSES, fieldPersonId).stream()
                .map(row -> DashboardCountDTO.builder().userName(row.getUserName()).count(row.getTicketCount()).build())
                .toList();
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public List<TicketIncidentTypeCountDTO> getTicketsByIncidentType(Long fieldPersonId) {
        return ticketRepository.countOpenTicketsByIncidentType(TicketQueueRules.QUEUE_STATUSES, fieldPersonId).stream()
                .map(row -> TicketIncidentTypeCountDTO.builder().type(row.getType()).count(row.getTicketCount()).build())
                .toList();
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public List<TicketStatusCountDTO> getTicketsByStatus(Long fieldPersonId) {
        Map<TicketStatus, Long> counts = new LinkedHashMap<>();
        TicketQueueRules.DASHBOARD_STATUSES.forEach(status -> counts.put(status, 0L));
        ticketRepository.countTicketsByStatus(TicketQueueRules.DASHBOARD_STATUSES, fieldPersonId)
                .forEach(row -> counts.put(row.getStatus(), value(row.getTicketCount())));
        return counts.entrySet().stream()
                .map(entry -> TicketStatusCountDTO.builder()
                        .status(entry.getKey().name())
                        .count(entry.getValue())
                        .build())
                .toList();
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public List<TicketAgeingBucketDTO> getTicketAgeingBuckets(Long fieldPersonId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        AGE_BUCKETS.forEach(bucket -> counts.put(bucket, 0L));
        ticketRepository.countOpenTicketsByAgeDay(
                        LocalDate.now(TicketQueueRules.REPORT_ZONE), TicketQueueRules.QUEUE_STATUS_NAMES, fieldPersonId)
                .forEach(row -> {
                    String bucket = ageingBucket(value(row.getAgeDays()));
                    counts.compute(bucket, (key, current) -> value(current) + value(row.getTicketCount()));
                });
        return counts.entrySet().stream()
                .map(entry -> TicketAgeingBucketDTO.builder()
                        .bucket(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public List<TicketTrendDTO> getTicketsTrend(int days, Long fieldPersonId) {
        if (days < 1 || days > 366) {
            throw new IllegalArgumentException("days must be between 1 and 366");
        }
        LocalDate today = LocalDate.now(TicketQueueRules.REPORT_ZONE);
        LocalDate firstDay = today.minusDays(days - 1L);
        LocalDateTime fromDate = firstDay.atStartOfDay();
        LocalDateTime untilDate = today.plusDays(1).atStartOfDay();
        Map<LocalDate, long[]> countsByDate = new LinkedHashMap<>();
        for (int offset = 0; offset < days; offset++) {
            countsByDate.put(firstDay.plusDays(offset), new long[2]);
        }

        ticketRepository.countTicketsCreatedByDay(fromDate, untilDate, fieldPersonId).forEach(row -> {
            long[] counts = countsByDate.get(parseDate(row.getDateKey()));
            if (counts != null) {
                counts[0] = value(row.getTicketCount());
            }
        });
        ticketRepository.countTicketsClosedByDay(fromDate, untilDate, fieldPersonId).forEach(row -> {
            long[] counts = countsByDate.get(parseDate(row.getDateKey()));
            if (counts != null) {
                counts[1] = value(row.getTicketCount());
            }
        });

        List<TicketTrendDTO> result = new ArrayList<>(days);
        countsByDate.forEach((date, counts) -> result.add(TicketTrendDTO.builder()
                .date(date)
                .created(counts[0])
                .closed(counts[1])
                .build()));
        return result;
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public List<TaskStatusCountDTO> getTasksByStatus(Long userId) {
        return taskRepository.countTasksByStatus(userId).stream()
                .map(row -> TaskStatusCountDTO.builder().status(row.getStatus().name()).count(row.getTaskCount()).build())
                .toList();
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public List<TaskAssigneeCountDTO> getTasksByAssignee(Long userId) {
        LocalDateTime overdueBefore = LocalDateTime.now(TicketQueueRules.REPORT_ZONE).minusHours(24);
        return taskRepository.countPendingTasksByAssignee(overdueBefore, userId).stream()
                .map(row -> TaskAssigneeCountDTO.builder()
                        .userName(row.getUserName())
                        .pending(value(row.getPending()))
                        .overdue(value(row.getOverdue()))
                        .build())
                .toList();
    }

    public Long scopedTaskUserId(AppUser user) {
        return user.getRole() == com.inventory.msp.model.UserRole.ADMIN
                || user.getRole() == com.inventory.msp.model.UserRole.COORDINATOR
                || user.getRole() == com.inventory.msp.model.UserRole.REVIEWER
                ? null
                : user.getId();
    }

    private static long value(Long value) {
        return value == null ? 0 : value;
    }

    static String ageingBucket(long ageDays) {
        if (ageDays < 1) {
            return "0-1d";
        }
        if (ageDays < 3) {
            return "1-3d";
        }
        if (ageDays < 7) {
            return "3-7d";
        }
        return "7d+";
    }

    private static LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException("Database returned an invalid ticket trend date: " + date, ex);
        }
    }
}
