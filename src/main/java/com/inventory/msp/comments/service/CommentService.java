package com.inventory.msp.comments.service;

import com.inventory.msp.comments.dto.CommentDto;
import com.inventory.msp.comments.dto.CommentNotificationDto;
import com.inventory.msp.comments.dto.CreateCommentRequest;
import com.inventory.msp.comments.dto.MentionableUserDto;
import com.inventory.msp.comments.model.Comment;
import com.inventory.msp.comments.model.EntityType;
import com.inventory.msp.comments.model.CommentMention;
import com.inventory.msp.comments.repository.CommentMentionRepository;
import com.inventory.msp.comments.repository.CommentNotificationRepository;
import com.inventory.msp.comments.repository.CommentRepository;
import com.inventory.msp.comments.repository.ThreadParticipantRepository;
import com.inventory.msp.exception.NotFoundException;
import com.inventory.msp.exception.BadRequestException;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CommentService {
    private static final Pattern USERNAME_MENTION = Pattern.compile("@([A-Za-z0-9._-]+)");

    private final CommentRepository commentRepository;
    private final CommentMentionRepository commentMentionRepository;
    private final ThreadParticipantRepository threadParticipantRepository;
    private final CommentNotificationRepository notificationRepository;
    private final CommentPermissionService permissionService;
    private final UserRepository userRepository;
    @Transactional(transactionManager = "transactionManager")
    public CommentDto createComment(CreateCommentRequest request) {
        AppUser author = currentUser();
        requireCanComment(request.getEntityType(), request.getEntityId(), author);
        List<AppUser> mentionedUsers = resolveRequestedMentions(
                request.getMentionedUserIds(), request.getBody(), author);
        return insertComment(request.getEntityType(), request.getEntityId(), request.getBody(), author, mentionedUsers);
    }

    @Transactional(transactionManager = "transactionManager")
    public CommentDto createTicketComment(Long ticketId, String body, AppUser author) {
        return createTicketComment(ticketId, body, List.of(), author);
    }

    @Transactional(transactionManager = "transactionManager")
    public CommentDto createTicketComment(Long ticketId, String body, List<Long> mentionedUserIds, AppUser author) {
        requireCanComment(EntityType.TICKET, ticketId, author);
        List<AppUser> mentionedUsers = resolveRequestedMentions(mentionedUserIds, body, author);
        return insertComment(EntityType.TICKET, ticketId, body, author, mentionedUsers);
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public boolean canComment(Long ticketId, AppUser user) {
        return canComment(EntityType.TICKET, ticketId, user);
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public boolean canComment(EntityType entityType, Long entityId, AppUser user) {
        return permissionService.canComment(entityType, entityId, user);
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public List<CommentDto> getComments(EntityType entityType, Long entityId) {
        AppUser user = currentUser();
        permissionService.requireRead(entityType, entityId, user);
        List<Comment> comments = commentRepository.findThread(entityType, entityId);
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        Map<Long, List<Long>> mentions = commentIds.isEmpty() ? Map.of()
                : commentMentionRepository.findByCommentIdInOrderByCommentIdAscMentionedUserIdAsc(commentIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        CommentMention::getCommentId,
                        java.util.stream.Collectors.mapping(CommentMention::getMentionedUserId,
                                java.util.stream.Collectors.toList())));
        return comments.stream()
                .map(comment -> toDto(comment, mentions.getOrDefault(comment.getId(), List.of())))
                .toList();
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public List<MentionableUserDto> searchMentionableUsers(
            String query, EntityType entityType, Long entityId) {
        AppUser user = currentUser();
        requireCanComment(entityType, entityId, user);
        String search = query == null ? "" : query.trim();
        return userRepository.searchActiveMentionableUsers(search, PageRequest.of(0, 10)).stream()
                .map(candidate -> new MentionableUserDto(
                        candidate.getId(),
                        candidate.getUsername(),
                        displayName(candidate),
                        candidate.getFullName(),
                        displayName(candidate),
                        candidate.getRole().name()))
                .toList();
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public List<CommentNotificationDto> getNotifications(boolean unreadOnly) {
        return notificationRepository.findForUser(currentUser().getId(), unreadOnly);
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public long getUnreadNotificationCount() {
        return notificationRepository.countUnread(currentUser().getId());
    }

    @Transactional(transactionManager = "transactionManager")
    public void markNotificationRead(Long notificationId) {
        int updated = notificationRepository.markReadForUser(
                notificationId, currentUser().getId(), LocalDateTime.now());
        if (updated == 0) {
            throw new NotFoundException("Notification not found");
        }
    }

    private AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        if (user.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user has no role");
        }
        return user;
    }

    private void requireCanComment(Long ticketId, AppUser user) {
        requireCanComment(EntityType.TICKET, ticketId, user);
    }

    private void requireCanComment(EntityType entityType, Long entityId, AppUser user) {
        if (!canComment(entityType, entityId, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can comment once you're mentioned");
        }
    }

    private List<AppUser> resolveBodyMentions(String body, AppUser author) {
        Matcher matcher = USERNAME_MENTION.matcher(body);
        LinkedHashSet<String> usernames = new LinkedHashSet<>();
        while (matcher.find()) {
            usernames.add(matcher.group(1));
        }

        List<AppUser> users = new ArrayList<>();
        for (String username : usernames) {
            AppUser mentionedUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BadRequestException("Mentioned user @" + username + " does not exist"));
            if (!mentionedUser.isActive()) {
                throw new BadRequestException("Mentioned user @" + username + " is inactive");
            }
            if (!mentionedUser.getId().equals(author.getId())) {
                users.add(mentionedUser);
            }
        }
        return users;
    }

    private List<AppUser> resolveRequestedMentions(List<Long> requestedIds, String body, AppUser author) {
        return requestedIds == null || requestedIds.isEmpty()
                ? resolveBodyMentions(body, author)
                : resolveMentionIds(requestedIds, author);
    }

    private List<AppUser> resolveMentionIds(List<Long> requestedIds, AppUser author) {
        LinkedHashSet<Long> mentionIds = new LinkedHashSet<>(
                requestedIds == null ? List.of() : requestedIds);
        mentionIds.remove(author.getId());
        List<AppUser> mentionedUsers = userRepository.findAllById(mentionIds);
        Map<Long, AppUser> usersById = mentionedUsers.stream()
                .collect(java.util.stream.Collectors.toMap(AppUser::getId, user -> user));
        for (Long mentionedId : mentionIds) {
            AppUser mentionedUser = usersById.get(mentionedId);
            if (mentionedUser == null) {
                throw new BadRequestException("Mentioned user " + mentionedId + " does not exist");
            }
            if (!mentionedUser.isActive()) {
                throw new BadRequestException("Mentioned user " + mentionedId + " is inactive");
            }
        }
        return mentionIds.stream().map(usersById::get).toList();
    }

    private CommentDto insertComment(EntityType entityType, Long entityId, String body,
                                     AppUser author, List<AppUser> mentionedUsers) {
        LocalDateTime createdAt = LocalDateTime.now();
        Comment comment = new Comment();
        comment.setEntityType(entityType);
        comment.setEntityId(entityId);
        comment.setAuthorId(author.getId());
        comment.setAuthorName(displayName(author));
        comment.setAuthorRole(author.getRole().name());
        comment.setBody(body);
        comment.setCreatedAt(createdAt);
        comment = commentRepository.insert(comment);

        for (AppUser mentionedUser : mentionedUsers) {
            commentMentionRepository.save(new CommentMention(comment.getId(), mentionedUser.getId()));
            threadParticipantRepository.upsertMention(
                    entityType, entityId, mentionedUser.getId(), author.getId(), createdAt);
            notificationRepository.insertMention(
                    mentionedUser.getId(), entityType, entityId, comment.getId(), createdAt);
        }
        return toDto(comment, mentionedUsers.stream().map(AppUser::getId).toList());
    }

    private String displayName(AppUser user) {
        return user.getFullName() == null || user.getFullName().isBlank()
                ? user.getUsername() : user.getFullName();
    }

    private CommentDto toDto(Comment comment, List<Long> mentionedUserIds) {
        return new CommentDto(
                comment.getId(), comment.getEntityType(), comment.getEntityId(), comment.getAuthorId(),
                comment.getAuthorName(), comment.getAuthorRole(), comment.getBody(),
                comment.getCreatedAt(), List.copyOf(mentionedUserIds));
    }
}
