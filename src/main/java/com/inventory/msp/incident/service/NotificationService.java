package com.inventory.msp.incident.service;

import com.inventory.msp.incident.dto.EmailNotificationRequest;
import com.inventory.msp.incident.dto.SmsNotificationRequest;
import com.inventory.msp.incident.model.NotificationSettings;
import com.inventory.msp.incident.model.Ticket;
import com.inventory.msp.incident.repository.NotificationSettingsRepository;
import com.inventory.msp.incident.repository.TicketRepository;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.UserRole;
import com.inventory.msp.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationService {

    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final NotificationSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    // dedupe map: key -> lastSentEpochMillis
    private final ConcurrentHashMap<String, Long> dedupe = new ConcurrentHashMap<>();
    private final long DEDUPE_WINDOW_MS = 2 * 60 * 1000L; // 2 minutes

    public NotificationService(EmailSender emailSender,
                               SmsSender smsSender,
                               NotificationSettingsRepository settingsRepository,
                               UserRepository userRepository,
                               TicketRepository ticketRepository) {
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    // ----------------- frontend-initiated endpoints -----------------
    public void sendEmailRequest(EmailNotificationRequest req) {
        validateEventType(req.getEventType());
        if (req.getTicketId() == null || req.getTicketId() <= 0) {
            throw new IllegalArgumentException("ticketId must be greater than 0");
        }
        String key = dedupeKey(req.getTicketId(), req.getEventType(), req.getTo(), "EMAIL");
        if (isDeduped(key)) {
            log.info("Deduped email request for key={}", key);
            return;
        }
        // Async send
        sendEmailAsync(req);
    }

    public void sendSmsRequest(SmsNotificationRequest req) {
        validateEventType(req.getEventType());
        if (req.getTicketId() == null || req.getTicketId() <= 0) {
            throw new IllegalArgumentException("ticketId must be greater than 0");
        }
        String key = dedupeKey(req.getTicketId(), req.getEventType(), req.getTo(), "SMS");
        if (isDeduped(key)) {
            log.info("Deduped sms request for key={}", key);
            return;
        }
        sendSmsAsync(req);
    }

    private void validateEventType(String eventType) {
        List<String> valid = Arrays.asList(
                "TICKET_CREATED",
                "TICKET_ACKNOWLEDGED",
                "TICKET_ASSIGNED",
                "TICKET_RESOLVED",
                "TICKET_REVALIDATION_REQUESTED",
                "TICKET_ON_HOLD",
                "TICKET_REOPENED",
                "TICKET_SENT_FOR_REVIEW",
                "TICKET_REJECTED"
        );
        if (eventType == null || !valid.contains(eventType)) {
            throw new IllegalArgumentException("Invalid eventType: " + eventType);
        }
    }

    private String dedupeKey(Long ticketId, String eventType, String to, String channel) {
        return ticketId + ":" + eventType + ":" + to + ":" + channel;
    }

    private boolean isDeduped(String key) {
        long now = Instant.now().toEpochMilli();
        Long prev = dedupe.get(key);
        if (prev != null && (now - prev) < DEDUPE_WINDOW_MS) {
            return true;
        }
        dedupe.put(key, now);
        return false;
    }

    // ----------------- async senders used by frontend endpoints -----------------
    @Async
    public void sendEmailAsync(EmailNotificationRequest req) {
        try {
            emailSender.send(req.getTo(), req.getSubject(), req.getBody());
            log.info("Email sent to={}, ticketId={}, eventType={}", req.getTo(), req.getTicketId(), req.getEventType());
        } catch (Exception ex) {
            log.error("Failed to send email to={} ticketId={} eventType={}: {}",
                    req.getTo(), req.getTicketId(), req.getEventType(), ex.getMessage());
            // bubble up as runtime so controller can return 500 when called synchronously
            throw new RuntimeException(ex.getMessage());
        }
    }

    @Async
    public void sendSmsAsync(SmsNotificationRequest req) {
        try {
            smsSender.send(req.getTo(), req.getMessage());
            log.info("SMS sent to={}, ticketId={}, eventType={}", req.getTo(), req.getTicketId(), req.getEventType());
        } catch (Exception ex) {
            log.error("Failed to send sms to={} ticketId={} eventType={}: {}",
                    req.getTo(), req.getTicketId(), req.getEventType(), ex.getMessage());
            throw new RuntimeException(ex.getMessage());
        }
    }

    // ----------------- automatic sends triggered from ticket actions -----------------
    public void notifyForTicketEvent(Long ticketId, String eventType) {
        try {
            validateEventType(eventType);
            Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
            if (ticket == null) {
                log.warn("Ticket not found for notification: {}", ticketId);
                return;
            }

            // Resolve recipients based on event type
            Set<UserContact> recipients = resolveRecipients(ticket, eventType);

            for (UserContact uc : recipients) {
                if (uc.email != null && uc.emailEnabled && uc.notifyEmailFor(eventType)) {
                    String key = dedupeKey(ticketId, eventType, uc.email, "EMAIL");
                    if (!isDeduped(key)) {
                        // fire-and-forget
                        try {
                            emailSender.sendAsync(uc.email, buildSubject(eventType, ticket), buildBody(ticket));
                            log.info("Auto email scheduled to={} ticketId={} eventType={}", uc.email, ticketId, eventType);
                        } catch (Exception e) {
                            log.error("Failed auto-email to {}: {}", uc.email, e.getMessage());
                        }
                    }
                }

                if (uc.phone != null && uc.smsEnabled && uc.notifySmsFor(eventType)) {
                    String key = dedupeKey(ticketId, eventType, uc.phone, "SMS");
                    if (!isDeduped(key)) {
                        try {
                            smsSender.sendAsync(uc.phone, buildSmsMessage(eventType, ticket));
                            log.info("Auto sms scheduled to={} ticketId={} eventType={}", uc.phone, ticketId, eventType);
                        } catch (Exception e) {
                            log.error("Failed auto-sms to {}: {}", uc.phone, e.getMessage());
                        }
                    }
                }
            }

        } catch (Exception ex) {
            // Never allow notifications to break ticket actions
            log.error("Error while notifying for ticket {} event {}: {}", ticketId, eventType, ex.getMessage());
        }
    }

    private String buildSubject(String eventType, Ticket ticket) {
        String prefix = switch (eventType) {
            case "TICKET_CREATED" -> "Ticket Created";
            case "TICKET_ACKNOWLEDGED" -> "Ticket Acknowledged";
            case "TICKET_ASSIGNED" -> "Reviewer Assigned";
            case "TICKET_RESOLVED" -> "Ticket Resolved";
            case "TICKET_REVALIDATION_REQUESTED" -> "Revalidation Requested";
            case "TICKET_ON_HOLD" -> "Ticket On Hold";
            case "TICKET_REOPENED" -> "Ticket Reopened";
            case "TICKET_SENT_FOR_REVIEW" -> "Sent for Review";
            case "TICKET_REJECTED" -> "Ticket Rejected";
            default -> "Ticket Update";
        };
        return String.format("%s #%d", prefix, ticket.getId());
    }

    private String buildBody(Ticket ticket) {
        String incident = ticket.getIncidentType() != null ? ticket.getIncidentType().getName() : "Incident";
        String location = ticket.getLocation() != null ? ticket.getLocation().getName() : "Unknown location";
        String priority = ticket.getPriority() != null ? ticket.getPriority().name() : "UNKNOWN";
        String status = ticket.getStatus() != null ? ticket.getStatus().name() : "UNKNOWN";
        return String.format("%s at %s (%s priority). Status: %s.", incident, location, priority, status);
    }

    private String buildSmsMessage(String eventType, Ticket ticket) {
        return String.format("%s #%d. %s", buildSubject(eventType, ticket), ticket.getId(), buildBody(ticket));
    }

    private Set<UserContact> resolveRecipients(Ticket ticket, String eventType) {
        Set<UserContact> out = new HashSet<>();

        if ("TICKET_REVALIDATION_REQUESTED".equals(eventType)) {
            AppUser raisedBy = ticket.getRaisedByUser();
            if (raisedBy != null) {
                NotificationSettings ns = settingsRepository.findByUsername(raisedBy.getUsername()).orElse(null);
                out.add(UserContact.fromAppUser(raisedBy, ns));
            }
            return out;
        }

        if ("TICKET_REOPENED".equals(eventType)) {
            if (ticket.getFieldPerson() != null && ticket.getFieldPerson().getUser() != null) {
                AppUser fpUser = ticket.getFieldPerson().getUser();
                NotificationSettings ns = settingsRepository.findByUsername(fpUser.getUsername()).orElse(null);
                out.add(UserContact.fromAppUser(fpUser, ns));
            }
            return out;
        }

        if ("TICKET_RESOLVED".equals(eventType) || "TICKET_SENT_FOR_REVIEW".equals(eventType)) {
            if (ticket.getReviewer() != null) {
                AppUser reviewer = ticket.getReviewer();
                NotificationSettings ns = settingsRepository.findByUsername(reviewer.getUsername()).orElse(null);
                out.add(UserContact.fromAppUser(reviewer, ns));
            }
            return out;
        }

        // SUPPORT_ENGINEER who raised the ticket
        AppUser raisedBy = ticket.getRaisedByUser();
        if (raisedBy != null) {
            NotificationSettings ns = settingsRepository.findByUsername(raisedBy.getUsername()).orElse(null);
            out.add(UserContact.fromAppUser(raisedBy, ns));
        }

        // FIELD_PERSON assigned
        if (ticket.getFieldPerson() != null) {
                    // FieldPerson may have a linked AppUser or a phone number
                    if (ticket.getFieldPerson().getUser() != null) {
                        AppUser fpUser = ticket.getFieldPerson().getUser();
                        NotificationSettings ns = settingsRepository.findByUsername(fpUser.getUsername()).orElse(null);
                        out.add(UserContact.fromAppUser(fpUser, ns));
                    } else if (ticket.getFieldPerson().getPhone() != null) {
                        // Create a contact from phone only
                        NotificationSettings ns = null;
                        UserContact uc = new UserContact();
                        uc.username = ticket.getFieldPerson().getName();
                        uc.email = null;
                        uc.phone = ticket.getFieldPerson().getPhone();
                        uc.emailEnabled = false;
                        uc.smsEnabled = true;
                        uc.settings = ns;
                        out.add(uc);
                    }
                }

        // REVIEWER
        if (ticket.getReviewer() != null) {
            AppUser reviewer = ticket.getReviewer();
            NotificationSettings ns = settingsRepository.findByUsername(reviewer.getUsername()).orElse(null);
            out.add(UserContact.fromAppUser(reviewer, ns));
        }

        // COORDINATOR
        if (ticket.getCoordinator() != null) {
            AppUser coordinator = ticket.getCoordinator();
            NotificationSettings ns = settingsRepository.findByUsername(coordinator.getUsername()).orElse(null);
            out.add(UserContact.fromAppUser(coordinator, ns));
        }

        // ADMINs for events that require admin notification. Which events include admins per spec:
        List<String> adminEvents = Arrays.asList("TICKET_CREATED", "TICKET_ACKNOWLEDGED", "TICKET_ASSIGNED", "TICKET_RESOLVED", "TICKET_REVALIDATION_REQUESTED", "TICKET_REOPENED", "TICKET_REJECTED", "TICKET_SENT_FOR_REVIEW");
        if (adminEvents.contains(eventType)) {
            List<AppUser> admins = userRepository.findByRole(UserRole.ADMIN);
            for (AppUser admin : admins) {
                NotificationSettings ns = settingsRepository.findByUsername(admin.getUsername()).orElse(null);
                out.add(UserContact.fromAppUser(admin, ns));
            }
        }

        return out;
    }

    // lightweight holder for contact + settings
    private static class UserContact {
        String username;
        String email;
        String phone;
        boolean emailEnabled;
        boolean smsEnabled;
        NotificationSettings settings;

        static UserContact fromAppUser(AppUser u, NotificationSettings ns) {
            UserContact uc = new UserContact();
            uc.username = u.getUsername();
                    // Contact details are stored in NotificationSettings; AppUser does not contain email/phone in this model
                    uc.email = ns != null ? ns.getEmail() : null;
                    uc.phone = ns != null ? ns.getPhone() : null;
                    uc.emailEnabled = ns == null ? false : Boolean.TRUE.equals(ns.getEmailEnabled());
                    uc.smsEnabled = ns == null ? false : Boolean.TRUE.equals(ns.getSmsEnabled());
                    uc.settings = ns;
                    return uc;
                }

        boolean notifyEmailFor(String eventType) {
            if (settings == null) return false;
            return settings.isNotificationEnabledForEvent(eventType, "EMAIL");
        }

        boolean notifySmsFor(String eventType) {
            if (settings == null) return false;
            return settings.isNotificationEnabledForEvent(eventType, "SMS");
        }
    }
}
