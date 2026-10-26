package com.inventory.msp.incident.repository;

import com.inventory.msp.incident.model.FieldPerson;
import com.inventory.msp.incident.model.Ticket;
import com.inventory.msp.incident.model.TicketQueueRules;
import com.inventory.msp.incident.model.TicketStatus;
import com.inventory.msp.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("SELECT fp.id AS userId, fp.name AS userName, assigneeUser.email AS email, " +
            "COUNT(t.id) AS openTicketCount, MIN(" + TicketQueueRules.TICKET_START_TIME_JPQL + ") AS oldestCreatedAt " +
            "FROM Ticket t LEFT JOIN t.fieldPerson fp LEFT JOIN fp.user assigneeUser " +
            "WHERE t.status = :status AND fp.id IS NOT NULL " +
            "AND (:fieldPersonId IS NULL OR fp.id = :fieldPersonId) " +
            "GROUP BY fp.id, fp.name, assigneeUser.email HAVING COUNT(t.id) > :threshold")
    List<AgeingAssigneeSummary> findAgeingDefaulters(
            @Param("status") TicketStatus status,
            @Param("threshold") long threshold,
            @Param("fieldPersonId") Long fieldPersonId);

    @Query("""
            SELECT t
            FROM Ticket t
            JOIN FETCH t.fieldPerson fp
            LEFT JOIN FETCH fp.user
            LEFT JOIN FETCH t.incidentType
            WHERE t.status = :status
              AND fp.id IN :fieldPersonIds
            ORDER BY fp.id ASC, COALESCE(t.createdAt, t.assignedAt) ASC
            """)
    List<Ticket> findAgeingTicketsForFieldPersons(
            @Param("status") TicketStatus status,
            @Param("fieldPersonIds") List<Long> fieldPersonIds);

    interface AgeingAssigneeSummary {
        Long getUserId();
        String getUserName();
        String getEmail();
        Long getOpenTicketCount();
        java.time.LocalDateTime getOldestCreatedAt();
    }

    @Query("SELECT t FROM Ticket t JOIN FETCH t.fieldPerson fp LEFT JOIN FETCH fp.user " +
            "JOIN FETCH t.incidentType WHERE t.status = :openStatus AND " +
            TicketQueueRules.TICKET_START_TIME_JPQL + " < :createdBefore AND t.flagged = false " +
            "AND NOT EXISTS (SELECT h.id FROM TicketHistory h WHERE h.ticket = t " +
            "AND (h.action IS NULL OR h.action <> com.inventory.msp.incident.model.TicketAction.TICKET_CREATED))")
    List<Ticket> findTicketsToFlag(
            @Param("openStatus") TicketStatus openStatus,
            @Param("createdBefore") LocalDateTime createdBefore);

    @Query("""
            SELECT t.id
            FROM Ticket t
            WHERE t.flagged = true
              AND (t.status <> :openStatus OR EXISTS (
                  SELECT h.id FROM TicketHistory h
                  WHERE h.ticket = t
                    AND (h.action IS NULL OR h.action <> com.inventory.msp.incident.model.TicketAction.TICKET_CREATED)
              ))
            """)
    List<Long> findFlaggedTicketIdsToClear(@Param("openStatus") TicketStatus openStatus);

    @Query("""
            SELECT t
            FROM Ticket t
            JOIN FETCH t.fieldPerson fp
            LEFT JOIN FETCH fp.user
            JOIN FETCH t.incidentType
            WHERE t.flagged = true
              AND t.status = :openStatus
              AND (t.lastReminderAt IS NULL OR t.lastReminderAt <= :reminderBefore)
              AND NOT EXISTS (
                  SELECT h.id FROM TicketHistory h
                  WHERE h.ticket = t
                    AND (h.action IS NULL OR h.action <> com.inventory.msp.incident.model.TicketAction.TICKET_CREATED)
              )
            """)
    List<Ticket> findFlaggedTicketsDueForReminder(
            @Param("openStatus") TicketStatus openStatus,
            @Param("reminderBefore") LocalDateTime reminderBefore);

    @Query("""
            SELECT t
            FROM Ticket t
            JOIN FETCH t.fieldPerson fp
            LEFT JOIN FETCH fp.user
            JOIN FETCH t.incidentType
            WHERE t.flagged = true
              AND t.status = :openStatus
              AND (:userId IS NULL OR fp.user.id = :userId)
            ORDER BY t.createdAt ASC
            """)
    List<Ticket> findFlaggedTickets(
            @Param("openStatus") TicketStatus openStatus,
            @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Ticket t SET t.flagged = false, t.version = t.version + 1 WHERE t.id IN :ticketIds")
    int clearFlags(@Param("ticketIds") List<Long> ticketIds);

    @Query(value = "SELECT COUNT(*) AS totalInQueue, " +
            "COALESCE(SUM(CASE WHEN t.flagged = 1 THEN 1 ELSE 0 END), 0) AS flaggedCount, " +
            "COALESCE(AVG(DATEDIFF(:today, DATE(" + TicketQueueRules.TICKET_START_TIME_SQL + "))), 0) AS avgAgeDays, " +
            "COALESCE(MAX(DATEDIFF(:today, DATE(" + TicketQueueRules.TICKET_START_TIME_SQL + "))), 0) AS oldestAgeDays " +
            "FROM tickets t LEFT JOIN field_persons fp ON fp.id = t.field_person_id " +
            "WHERE t.status IN (:queueStatuses) " +
            "AND (CAST(:fieldPersonId AS SIGNED) IS NULL OR fp.id = CAST(:fieldPersonId AS SIGNED))",
            nativeQuery = true)
    TicketDashboardAggregate getDashboardAggregate(
            @Param("today") java.time.LocalDate today,
            @Param("queueStatuses") List<String> queueStatuses,
            @Param("fieldPersonId") Long fieldPersonId);

    @Query("""
            SELECT COALESCE(fp.name, 'Unassigned') AS userName, COUNT(t.id) AS ticketCount
            FROM Ticket t LEFT JOIN t.fieldPerson fp
            WHERE t.status IN :queueStatuses
              AND (:fieldPersonId IS NULL OR fp.id = :fieldPersonId)
            GROUP BY fp.id, COALESCE(fp.name, 'Unassigned')
            ORDER BY COUNT(t.id) DESC
            """)
    List<TicketAssigneeCount> countOpenTicketsByAssignee(
            @Param("queueStatuses") List<TicketStatus> queueStatuses,
            @Param("fieldPersonId") Long fieldPersonId);

    @Query("""
            SELECT t.incidentType.name AS type, COUNT(t.id) AS ticketCount
            FROM Ticket t
            LEFT JOIN t.fieldPerson fp
            WHERE t.status IN :queueStatuses
              AND (:fieldPersonId IS NULL OR fp.id = :fieldPersonId)
            GROUP BY t.incidentType.id, t.incidentType.name
            ORDER BY COUNT(t.id) DESC
            """)
    List<TicketIncidentTypeCount> countOpenTicketsByIncidentType(
            @Param("queueStatuses") List<TicketStatus> queueStatuses,
            @Param("fieldPersonId") Long fieldPersonId);

    @Query("""
            SELECT t.status AS status, COUNT(t.id) AS ticketCount
            FROM Ticket t
            LEFT JOIN t.fieldPerson fp
            WHERE t.status IN :statuses
              AND (:fieldPersonId IS NULL OR fp.id = :fieldPersonId)
            GROUP BY t.status
            """)
    List<TicketStatusCount> countTicketsByStatus(
            @Param("statuses") List<TicketStatus> statuses,
            @Param("fieldPersonId") Long fieldPersonId);

    @Query(value = "SELECT DATEDIFF(:today, DATE(" + TicketQueueRules.TICKET_START_TIME_SQL + ")) AS ageDays, " +
            "COUNT(*) AS ticketCount FROM tickets t " +
            "LEFT JOIN field_persons fp ON fp.id = t.field_person_id " +
            "WHERE t.status IN (:queueStatuses) " +
            "AND (CAST(:fieldPersonId AS SIGNED) IS NULL OR fp.id = CAST(:fieldPersonId AS SIGNED)) " +
            "GROUP BY DATEDIFF(:today, DATE(" + TicketQueueRules.TICKET_START_TIME_SQL + "))",
            nativeQuery = true)
    List<TicketAgeDayCount> countOpenTicketsByAgeDay(
            @Param("today") java.time.LocalDate today,
            @Param("queueStatuses") List<String> queueStatuses,
            @Param("fieldPersonId") Long fieldPersonId);

    @Query(value = "SELECT DATE_FORMAT(" + TicketQueueRules.TICKET_START_TIME_SQL +
            ", '%Y-%m-%d') AS dateKey, COUNT(*) AS ticketCount FROM tickets t " +
            "LEFT JOIN field_persons fp ON fp.id = t.field_person_id " +
            "WHERE " + TicketQueueRules.TICKET_START_TIME_SQL + " >= :fromDate " +
            "AND " + TicketQueueRules.TICKET_START_TIME_SQL + " < :untilDate " +
            "AND (CAST(:fieldPersonId AS SIGNED) IS NULL OR fp.id = CAST(:fieldPersonId AS SIGNED)) " +
            "GROUP BY DATE_FORMAT(" + TicketQueueRules.TICKET_START_TIME_SQL + ", '%Y-%m-%d')",
            nativeQuery = true)
    List<TicketDailyCount> countTicketsCreatedByDay(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("untilDate") LocalDateTime untilDate,
            @Param("fieldPersonId") Long fieldPersonId);

    @Query(value = """
            SELECT DATE_FORMAT(t.closedAt, '%Y-%m-%d') AS dateKey,
                   COUNT(*) AS ticketCount
            FROM tickets t
            LEFT JOIN field_persons fp ON fp.id = t.field_person_id
            WHERE t.closedAt >= :fromDate
              AND t.closedAt < :untilDate
              AND (CAST(:fieldPersonId AS SIGNED) IS NULL OR fp.id = CAST(:fieldPersonId AS SIGNED))
            GROUP BY DATE_FORMAT(t.closedAt, '%Y-%m-%d')
            """, nativeQuery = true)
    List<TicketDailyCount> countTicketsClosedByDay(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("untilDate") LocalDateTime untilDate,
            @Param("fieldPersonId") Long fieldPersonId);

    interface TicketDashboardAggregate {
        Long getTotalInQueue();
        Long getFlaggedCount();
        Double getAvgAgeDays();
        Long getOldestAgeDays();
    }

    interface TicketAssigneeCount {
        String getUserName();
        Long getTicketCount();
    }

    interface TicketIncidentTypeCount {
        String getType();
        Long getTicketCount();
    }

    interface TicketAgeDayCount {
        Long getAgeDays();
        Long getTicketCount();
    }

    interface TicketDailyCount {
        String getDateKey();
        Long getTicketCount();
    }

    interface TicketStatusCount {
        TicketStatus getStatus();
        Long getTicketCount();
    }

    @Query("""
            SELECT t.id AS ticketId,
                   t.incidentType.name AS title,
                   t.location.name AS location,
                   COALESCE(t.createdAt, t.assignedAt) AS ticketStartAt,
                   t.flagged AS flagged
            FROM Ticket t
            JOIN t.fieldPerson fp
            WHERE fp.user.id = :userId
              AND t.status = :status
            ORDER BY COALESCE(t.createdAt, t.assignedAt) ASC
            """)
    List<MyPendingTicket> findMyPendingTickets(
            @Param("userId") Long userId,
            @Param("status") TicketStatus status);

    interface MyPendingTicket {
        Long getTicketId();
        String getTitle();
        String getLocation();
        LocalDateTime getTicketStartAt();
        boolean getFlagged();
    }

    // List tickets raised by a specific user
    Page<Ticket> findByRaisedByUser(AppUser user, Pageable pageable);

    // List tickets by status
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    // List tickets assigned to a specific reviewer
    Page<Ticket> findByReviewerAndStatusIn(AppUser reviewer, List<TicketStatus> statuses, Pageable pageable);

    // List all tickets ever assigned to a specific reviewer (ALL history)
    Page<Ticket> findByReviewerOrderByCreatedAtDesc(AppUser reviewer, Pageable pageable);

    // List all tickets ever handled by a specific coordinator
    Page<Ticket> findByCoordinatorOrderByCreatedAtDesc(AppUser coordinator, Pageable pageable);

    // List all tickets ever assigned to a specific field person
    Page<Ticket> findByFieldPersonOrderByCreatedAtDesc(FieldPerson fieldPerson, Pageable pageable);

    // List all tickets (admin)
    Page<Ticket> findAll(Pageable pageable);

    // Count by status
    long countByStatus(TicketStatus status);

    // Find tickets in multiple statuses
    List<Ticket> findByStatusIn(List<TicketStatus> statuses);

    // Find tickets awaiting coordinator action (OPEN or REOPENED)
    @Query("SELECT t FROM Ticket t WHERE t.status IN :statuses ORDER BY t.createdAt ASC")
    List<Ticket> findByStatusInOrderByCreatedAtAsc(@Param("statuses") List<TicketStatus> statuses);

    // Find tickets assigned to a specific field person in specific statuses
    @Query("SELECT t FROM Ticket t WHERE t.fieldPerson = :fieldPerson AND t.status IN :statuses ORDER BY t.createdAt ASC")
    List<Ticket> findByFieldPersonAndStatusInOrderByCreatedAtAsc(@Param("fieldPerson") FieldPerson fieldPerson, @Param("statuses") List<TicketStatus> statuses);

    // Advanced filtering for Admin dashboard
    @Query("SELECT t FROM Ticket t WHERE " +
           "(:raisedByUserId IS NULL OR t.raisedByUser.id = :raisedByUserId) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:incidentTypeId IS NULL OR t.incidentType.id = :incidentTypeId) AND " +
           "(:locationId IS NULL OR t.location.id = :locationId) AND " +
           "(:fromDate IS NULL OR t.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR t.createdAt <= :toDate)")
    Page<Ticket> findWithFilters(
            @Param("raisedByUserId") Long raisedByUserId,
            @Param("status") TicketStatus status,
            @Param("incidentTypeId") Long incidentTypeId,
            @Param("locationId") Long locationId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    // Find tickets created within a date range (for daily dashboard counts)
    @Query("SELECT t FROM Ticket t WHERE t.createdAt >= :fromDate AND t.createdAt <= :toDate ORDER BY t.createdAt ASC")
    List<Ticket> findByCreatedAtBetween(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);
}
