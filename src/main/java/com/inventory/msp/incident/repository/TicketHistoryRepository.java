package com.inventory.msp.incident.repository;

import com.inventory.msp.incident.model.Ticket;
import com.inventory.msp.incident.model.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {

    List<TicketHistory> findByTicketOrderByChangedAtDesc(Ticket ticket);
}

