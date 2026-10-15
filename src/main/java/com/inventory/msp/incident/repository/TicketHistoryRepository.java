package com.inventory.msp.incident.repository;

import com.inventory.msp.incident.model.Ticket;
import com.inventory.msp.incident.model.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {

    List<TicketHistory> findByTicketOrderByChangedAtDesc(Ticket ticket);

    List<TicketHistory> findByTicketOrderByPerformedAtDescIdDesc(Ticket ticket);

    @Query("""
            SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END
            FROM TicketHistory h
            WHERE h.ticket.id = :ticketId
              AND (h.action IS NULL OR h.action <> com.inventory.msp.incident.model.TicketAction.TICKET_CREATED)
            """)
    boolean hasNonCreationAction(@Param("ticketId") Long ticketId);
}
