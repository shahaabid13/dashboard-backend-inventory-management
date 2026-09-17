package com.inventory.msp.incident.repository;

import com.inventory.msp.incident.model.FieldPerson;
import com.inventory.msp.incident.model.Ticket;
import com.inventory.msp.incident.model.TicketStatus;
import com.inventory.msp.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

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

