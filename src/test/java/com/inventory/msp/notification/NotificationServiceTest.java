package com.inventory.msp.notification;

import com.inventory.msp.incident.repository.NotificationSettingsRepository;
import com.inventory.msp.incident.repository.TicketRepository;
import com.inventory.msp.incident.service.EmailSender;
import com.inventory.msp.incident.service.NotificationService;
import com.inventory.msp.incident.service.SmsSender;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.notification.model.InAppNotification;
import com.inventory.msp.notification.repository.InAppNotificationRepository;
import com.inventory.msp.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    @Test
    void storesNotificationAgainstLoginUsername() {
        InAppNotificationRepository repository = mock(InAppNotificationRepository.class);
        when(repository.save(any(InAppNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        NotificationService service = service(repository);

        InAppNotification notification = service.createInAppNotification(
                38L, "muzamil", "TICKET_REMINDER", 34L, "Ticket pending",
                "Ticket #34 has had no action for 43 hours");

        assertEquals(38L, notification.getUserId());
        assertEquals("muzamil", notification.getUsername());
        assertEquals("TICKET_REMINDER", notification.getType());
        assertEquals(34L, notification.getReferenceId());
        verify(repository).save(any(InAppNotification.class));
    }

    @Test
    void unreadCountDropsAfterTheOwnerMarksNotificationRead() {
        InAppNotificationRepository repository = mock(InAppNotificationRepository.class);
        AtomicBoolean unread = new AtomicBoolean(true);
        when(repository.countByUsernameAndReadFalse("muzamil")).thenAnswer(ignored -> unread.get() ? 1L : 0L);
        when(repository.markReadForUser(15L, "muzamil"))
                .thenAnswer(ignored -> unread.compareAndSet(true, false) ? 1 : 0);
        NotificationService service = service(repository);

        assertEquals(1, service.getUnreadNotificationCount("muzamil"));
        assertTrue(service.markNotificationRead("muzamil", 15L));
        assertEquals(0, service.getUnreadNotificationCount("muzamil"));
    }

    @Test
    void unreadCountIsFilteredByLoginUsername() {
        InAppNotificationRepository repository = mock(InAppNotificationRepository.class);
        when(repository.countByUsernameAndReadFalse("muzamil")).thenReturn(1L);
        when(repository.countByUsernameAndReadFalse("saqib")).thenReturn(0L);
        NotificationService service = service(repository);

        assertEquals(1, service.getUnreadNotificationCount("muzamil"));
        assertEquals(0, service.getUnreadNotificationCount("saqib"));
    }

    @Test
    void userCannotMarkAnotherUsersNotificationRead() {
        InAppNotificationRepository repository = mock(InAppNotificationRepository.class);
        when(repository.markReadForUser(15L, "muzamil")).thenReturn(0);
        NotificationService service = service(repository);

        assertFalse(service.markNotificationRead("muzamil", 15L));
        verify(repository).markReadForUser(15L, "muzamil");
    }

    private NotificationService service(InAppNotificationRepository repository) {
        return new NotificationService(
                mock(EmailSender.class),
                mock(SmsSender.class),
                mock(NotificationSettingsRepository.class),
                repository,
                mock(UserRepository.class),
                mock(TicketRepository.class));
    }
}
