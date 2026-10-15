package com.inventory.msp.incident.service;

import com.inventory.msp.incident.dto.TicketStatusCountDTO;
import com.inventory.msp.incident.dto.TicketTrendDTO;
import com.inventory.msp.incident.model.TicketQueueRules;
import com.inventory.msp.incident.repository.TicketRepository;
import com.inventory.msp.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CimsDashboardServiceTest {

    @Test
    void ageingBucketsUseTheExpectedDayBoundaries() {
        assertEquals("0-1d", CimsDashboardService.ageingBucket(0));
        assertEquals("1-3d", CimsDashboardService.ageingBucket(1));
        assertEquals("1-3d", CimsDashboardService.ageingBucket(2));
        assertEquals("3-7d", CimsDashboardService.ageingBucket(3));
        assertEquals("3-7d", CimsDashboardService.ageingBucket(6));
        assertEquals("7d+", CimsDashboardService.ageingBucket(7));
    }

    @Test
    void ticketStatusChartReturnsEveryDashboardStatusIncludingZeroCounts() {
        TicketRepository ticketRepository = mock(TicketRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        when(ticketRepository.countTicketsByStatus(
                TicketQueueRules.DASHBOARD_STATUSES, null)).thenReturn(List.of());
        CimsDashboardService service = new CimsDashboardService(ticketRepository, taskRepository);

        List<TicketStatusCountDTO> result = service.getTicketsByStatus(null);

        assertEquals(List.of("OPEN", "PENDING", "ASSIGNED_TO_REVIEWER", "REVALIDATION", "RESOLVED"),
                result.stream().map(TicketStatusCountDTO::getStatus).toList());
        assertEquals(List.of(0L, 0L, 0L, 0L, 0L), result.stream().map(TicketStatusCountDTO::getCount).toList());
    }

    @Test
    void trendUsesCreatedAndClosedDailyCountsReturnedByRepository() {
        TicketRepository ticketRepository = mock(TicketRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        TicketRepository.TicketDailyCount created = mock(TicketRepository.TicketDailyCount.class);
        TicketRepository.TicketDailyCount closed = mock(TicketRepository.TicketDailyCount.class);
        LocalDate today = LocalDate.now(TicketQueueRules.REPORT_ZONE);
        String dateKey = today.toString();
        when(created.getDateKey()).thenReturn(dateKey);
        when(created.getTicketCount()).thenReturn(4L);
        when(closed.getDateKey()).thenReturn(dateKey);
        when(closed.getTicketCount()).thenReturn(2L);
        when(ticketRepository.countTicketsCreatedByDay(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay(), null)).thenReturn(List.of(created));
        when(ticketRepository.countTicketsClosedByDay(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay(), null)).thenReturn(List.of(closed));
        CimsDashboardService service = new CimsDashboardService(ticketRepository, taskRepository);

        List<TicketTrendDTO> trend = service.getTicketsTrend(1, null);

        assertEquals(1, trend.size());
        assertEquals(today, trend.get(0).getDate());
        assertEquals(4, trend.get(0).getCreated());
        assertEquals(2, trend.get(0).getClosed());
    }
}
