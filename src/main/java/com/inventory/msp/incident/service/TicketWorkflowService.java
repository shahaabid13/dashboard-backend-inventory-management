package com.inventory.msp.incident.service;

import com.inventory.msp.exception.NotFoundException;
import com.inventory.msp.incident.exception.InvalidTicketStateTransitionException;
import com.inventory.msp.incident.model.FieldPerson;
import com.inventory.msp.incident.model.Ticket;
import com.inventory.msp.incident.model.TicketAction;
import com.inventory.msp.incident.model.TicketStatus;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.UserRole;
import com.inventory.msp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TicketWorkflowService {

    private final UserRepository userRepository;

    private static final Map<TicketAction, WorkflowRule> WORKFLOW = Map.of(
            TicketAction.RESOLVED, new WorkflowRule(
                    TicketAction.RESOLVED,
                    UserRole.FIELD_PERSON,
                    Set.of(TicketStatus.OPEN, TicketStatus.REOPENED),
                    TicketStatus.ASSIGNED_TO_REVIEWER),
            TicketAction.REVALIDATION_REQUESTED, new WorkflowRule(
                    TicketAction.REVALIDATION_REQUESTED,
                    UserRole.FIELD_PERSON,
                    Set.of(TicketStatus.OPEN, TicketStatus.REOPENED),
                    TicketStatus.REVALIDATION),
            TicketAction.REOPENED, new WorkflowRule(
                    TicketAction.REOPENED,
                    UserRole.SUPPORT_ENGINEER,
                    Set.of(TicketStatus.REVALIDATION),
                    TicketStatus.REOPENED),
            TicketAction.SENT_FOR_REVIEW, new WorkflowRule(
                    TicketAction.SENT_FOR_REVIEW,
                    UserRole.SUPPORT_ENGINEER,
                    Set.of(TicketStatus.REVALIDATION),
                    TicketStatus.ASSIGNED_TO_REVIEWER),
            TicketAction.REASSIGNED, new WorkflowRule(
                    TicketAction.REASSIGNED,
                    UserRole.SUPPORT_ENGINEER,
                    Set.of(TicketStatus.OPEN),
                    TicketStatus.OPEN),
            TicketAction.TICKET_CREATED, new WorkflowRule(
                    TicketAction.TICKET_CREATED,
                    UserRole.SUPPORT_ENGINEER,
                    Set.of(TicketStatus.OPEN),
                    TicketStatus.OPEN)
    );

    public WorkflowRule getRule(TicketAction action) {
        return WORKFLOW.get(action);
    }

    public void validateTransition(Ticket ticket, AppUser actor, TicketAction action) {
        if (ticket == null) {
            throw new NotFoundException("Ticket not found");
        }
        WorkflowRule rule = WORKFLOW.get(action);
        if (rule == null) {
            throw new InvalidTicketStateTransitionException("Unsupported ticket action: " + action);
        }
        if (actor == null || actor.getRole() == null) {
            throw new InvalidTicketStateTransitionException("Actor role is required for this action");
        }
        if (!rule.actorRole.equals(actor.getRole())) {
            throw new InvalidTicketStateTransitionException(
                    "Ticket action " + action + " is only allowed for " + rule.actorRole + " users");
        }
        if (!rule.allowedStatuses.contains(ticket.getStatus())) {
            throw new InvalidTicketStateTransitionException(
                    "Ticket #" + ticket.getId() + " is in status " + ticket.getStatus() + "; this action is only allowed in " + rule.allowedStatuses);
        }
        if (UserRole.FIELD_PERSON.equals(rule.actorRole)) {
            FieldPerson assigned = ticket.getFieldPerson();
            if (assigned == null || assigned.getUser() == null || !Objects.equals(assigned.getUser().getId(), actor.getId())) {
                throw new InvalidTicketStateTransitionException("Only the assigned field person can perform this action");
            }
        }
        if (UserRole.SUPPORT_ENGINEER.equals(rule.actorRole)) {
            if (ticket.getRaisedByUser() == null || !Objects.equals(ticket.getRaisedByUser().getId(), actor.getId())) {
                throw new InvalidTicketStateTransitionException("Only the ticket raiser can perform this support-engineer action");
            }
        }
    }

    public TicketStatus resolveStatus(TicketAction action) {
        WorkflowRule rule = WORKFLOW.get(action);
        if (rule == null) {
            throw new InvalidTicketStateTransitionException("Unsupported ticket action: " + action);
        }
        return rule.targetStatus;
    }

    public AppUser resolveAssignee(Ticket ticket, TicketAction action) {
        if (TicketAction.REVALIDATION_REQUESTED.equals(action)) {
            if (ticket.getRaisedByUser() != null) {
                return ticket.getRaisedByUser();
            }
            throw new NotFoundException("Ticket raiser not found");
        }
        if (TicketAction.REASSIGNED.equals(action)) {
            if (ticket.getFieldPerson() != null && ticket.getFieldPerson().getUser() != null) {
                return ticket.getFieldPerson().getUser();
            }
            throw new NotFoundException("Assigned field person not found");
        }
        if (TicketAction.REOPENED.equals(action)) {
            if (ticket.getFieldPerson() != null && ticket.getFieldPerson().getUser() != null) {
                return ticket.getFieldPerson().getUser();
            }
            if (ticket.getFieldPerson() != null) {
                return null;
            }
            throw new NotFoundException("Assigned field person not found");
        }
        if (TicketAction.RESOLVED.equals(action) || TicketAction.SENT_FOR_REVIEW.equals(action)) {
            if (ticket.getReviewer() != null) {
                return ticket.getReviewer();
            }
            List<AppUser> reviewers = userRepository.findByRole(UserRole.REVIEWER);
            if (reviewers == null || reviewers.isEmpty()) {
                throw new NotFoundException("No reviewer is configured for this ticket");
            }
            return reviewers.get(0);
        }
        if (TicketAction.TICKET_CREATED.equals(action)) {
            return ticket.getFieldPerson() != null ? ticket.getFieldPerson().getUser() : null;
        }
        return null;
    }

    public List<String> allowedActionsFor(Ticket ticket, AppUser actor) {
        if (ticket == null || actor == null || actor.getRole() == null) {
            return List.of();
        }
        if (UserRole.FIELD_PERSON.equals(actor.getRole())
                && ticket.getFieldPerson() != null
                && ticket.getFieldPerson().getUser() != null
                && Objects.equals(ticket.getFieldPerson().getUser().getId(), actor.getId())
                && Set.of(TicketStatus.OPEN, TicketStatus.REOPENED).contains(ticket.getStatus())) {
            return List.of("RESOLVED", "REVALIDATION");
        }
        if (UserRole.SUPPORT_ENGINEER.equals(actor.getRole())
                && ticket.getRaisedByUser() != null
                && Objects.equals(ticket.getRaisedByUser().getId(), actor.getId())
                && TicketStatus.REVALIDATION.equals(ticket.getStatus())) {
            return List.of("REOPEN", "SEND_FOR_REVIEW");
        }
        if (UserRole.SUPPORT_ENGINEER.equals(actor.getRole())
                && ticket.getRaisedByUser() != null
                && Objects.equals(ticket.getRaisedByUser().getId(), actor.getId())
                && TicketStatus.OPEN.equals(ticket.getStatus())) {
            return List.of("REASSIGN");
        }
        return List.of();
    }

    public static class WorkflowRule {
        private final TicketAction action;
        private final UserRole actorRole;
        private final Set<TicketStatus> allowedStatuses;
        private final TicketStatus targetStatus;

        public WorkflowRule(TicketAction action, UserRole actorRole, Set<TicketStatus> allowedStatuses, TicketStatus targetStatus) {
            this.action = action;
            this.actorRole = actorRole;
            this.allowedStatuses = allowedStatuses;
            this.targetStatus = targetStatus;
        }

        public TicketAction getAction() {
            return action;
        }

        public UserRole getActorRole() {
            return actorRole;
        }

        public Set<TicketStatus> getAllowedStatuses() {
            return allowedStatuses;
        }

        public TicketStatus getTargetStatus() {
            return targetStatus;
        }
    }
}
