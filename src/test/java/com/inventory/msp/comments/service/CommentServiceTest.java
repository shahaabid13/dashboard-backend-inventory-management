package com.inventory.msp.comments.service;

import com.inventory.msp.comments.dto.CommentDto;
import com.inventory.msp.comments.dto.CreateCommentRequest;
import com.inventory.msp.comments.model.Comment;
import com.inventory.msp.comments.model.EntityType;
import com.inventory.msp.comments.model.CommentMention;
import com.inventory.msp.comments.repository.CommentMentionRepository;
import com.inventory.msp.comments.repository.CommentNotificationRepository;
import com.inventory.msp.comments.repository.CommentRepository;
import com.inventory.msp.comments.repository.ThreadParticipantRepository;
import com.inventory.msp.incident.repository.TicketRepository;
import com.inventory.msp.incident.model.Ticket;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.UserRole;
import com.inventory.msp.repository.UserRepository;
import com.inventory.msp.task.model.Task;
import com.inventory.msp.task.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommentServiceTest {
    private CommentRepository commentRepository;
    private CommentMentionRepository mentionRepository;
    private ThreadParticipantRepository participantRepository;
    private CommentNotificationRepository notificationRepository;
    private CommentPermissionService permissionService;
    private TaskRepository taskRepository;
    private TicketRepository ticketRepository;
    private UserRepository userRepository;
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        mentionRepository = mock(CommentMentionRepository.class);
        participantRepository = mock(ThreadParticipantRepository.class);
        notificationRepository = mock(CommentNotificationRepository.class);
        taskRepository = mock(TaskRepository.class);
        ticketRepository = mock(TicketRepository.class);
        userRepository = mock(UserRepository.class);
        permissionService = new CommentPermissionService(
                taskRepository, ticketRepository, mentionRepository);
        commentService = new CommentService(commentRepository, mentionRepository, participantRepository,
                notificationRepository, permissionService, userRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("authenticated-user", "password", List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authorIsAlwaysDerivedFromAuthenticatedUser() {
        AppUser authenticated = user(11L, "authenticated-user", "Real Author", UserRole.REVIEWER);
        when(userRepository.findByUsername("authenticated-user")).thenReturn(Optional.of(authenticated));
        when(taskRepository.findById(50L)).thenReturn(Optional.of(new Task()));
        when(mentionRepository.countForEntityAndMentionedUser("TASK", 50L, 11L)).thenReturn(1L);
        when(commentRepository.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(100L);
            return comment;
        });
        CreateCommentRequest request = request(50L, "Hello", List.of());

        CommentDto result = commentService.createComment(request);

        assertEquals(11L, result.authorId());
        assertEquals("Real Author", result.authorName());
        assertEquals("REVIEWER", result.authorRole());
        verify(commentRepository).insert(argThat(comment ->
                comment.getAuthorId().equals(11L)
                        && comment.getAuthorName().equals("Real Author")
                        && comment.getAuthorRole().equals("REVIEWER")));
    }

    @Test
    void unmentionedTaskUserIsForbiddenFromWriting() {
        AppUser viewer = user(22L, "viewer", "Viewer", UserRole.VIEWER);
        when(taskRepository.findById(50L)).thenReturn(Optional.of(new Task()));
        when(mentionRepository.countForEntityAndMentionedUser("TASK", 50L, 22L)).thenReturn(0L);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> permissionService.requireWrite(EntityType.TASK, 50L, viewer))
                .getStatusCode().value());
    }

    @Test
    void duplicateMentionsAreDeduplicatedAndSelfMentionIsDroppedAndRecipientGetsThreadAccess() {
        AppUser author = user(11L, "authenticated-user", "Author", UserRole.REVIEWER);
        AppUser mentioned = user(22L, "mentioned", "Mentioned User", UserRole.FIELD_PERSON);
        when(userRepository.findByUsername("authenticated-user")).thenReturn(Optional.of(author));
        when(userRepository.findAllById(any())).thenReturn(List.of(mentioned));
        when(taskRepository.findById(50L)).thenReturn(Optional.of(new Task()));
        when(mentionRepository.countForEntityAndMentionedUser("TASK", 50L, 11L)).thenReturn(1L);
        when(commentRepository.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(100L);
            return comment;
        });
        CreateCommentRequest request = request(50L, "Mentioning users", List.of(11L, 22L, 22L));

        CommentDto result = commentService.createComment(request);

        assertEquals(List.of(22L), result.mentionedUserIds());
        verify(mentionRepository).save(argThat(mention ->
                mention.getCommentId().equals(100L)
                        && mention.getMentionedUserId().equals(22L)));
        verify(participantRepository).upsertMention(
                eq(EntityType.TASK), eq(50L), eq(22L), eq(11L), any());
        verify(notificationRepository).insertMention(
                eq(22L), eq(EntityType.TASK), eq(50L), eq(100L), any());

        assertDoesNotThrow(() -> permissionService.requireRead(EntityType.TASK, 50L, mentioned));
        verify(taskRepository, times(2)).findById(50L);
    }

    @Test
    void previouslyMentionedUserCanCommentAgain() {
        AppUser mentioned = user(22L, "mentioned", "Mentioned User", UserRole.FIELD_PERSON);
        when(ticketRepository.findById(70L)).thenReturn(Optional.of(ticket(70L, 11L)));
        when(mentionRepository.countForEntityAndMentionedUser("TICKET", 70L, 22L)).thenReturn(1L);
        when(commentRepository.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(101L);
            return comment;
        });

        assertDoesNotThrow(() -> commentService.createTicketComment(70L, "Another reply", mentioned));
        verify(commentRepository).insert(any(Comment.class));
    }

    @Test
    void nonMentionedUserGetsForbiddenWhenPosting() {
        AppUser user = user(22L, "user", "User", UserRole.FIELD_PERSON);
        when(ticketRepository.findById(70L)).thenReturn(Optional.of(ticket(70L, 11L)));
        when(mentionRepository.countForEntityAndMentionedUser("TICKET", 70L, 22L)).thenReturn(0L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> commentService.createTicketComment(70L, "A reply", user));

        assertEquals(403, exception.getStatusCode().value());
        verify(commentRepository, never()).insert(any());
    }

    @Test
    void genericCommentEndpointAlsoRejectsAnUntaggedTicketUser() {
        AppUser user = user(22L, "authenticated-user", "User", UserRole.SUPPORT_ENGINEER);
        when(userRepository.findByUsername("authenticated-user")).thenReturn(Optional.of(user));
        when(ticketRepository.findById(70L)).thenReturn(Optional.of(ticket(70L, 11L)));
        when(mentionRepository.countForEntityAndMentionedUser("TICKET", 70L, 22L)).thenReturn(0L);

        CreateCommentRequest request = request(70L, "Not tagged", List.of());
        request.setEntityType(EntityType.TICKET);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> commentService.createComment(request));

        assertEquals(403, exception.getStatusCode().value());
        verify(commentRepository, never()).insert(any());
    }

    @Test
    void genericCommentEndpointRejectsAnUntaggedTaskUser() {
        AppUser user = user(22L, "authenticated-user", "User", UserRole.SUPPORT_ENGINEER);
        when(userRepository.findByUsername("authenticated-user")).thenReturn(Optional.of(user));
        when(taskRepository.findById(19L)).thenReturn(Optional.of(new Task()));
        when(mentionRepository.countForEntityAndMentionedUser("TASK", 19L, 22L)).thenReturn(0L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> commentService.createComment(request(19L, "Not tagged", List.of())));

        assertEquals(403, exception.getStatusCode().value());
        verify(commentRepository, never()).insert(any());
    }

    @Test
    void previouslyMentionedTaskUserCanPostAgain() {
        AppUser user = user(22L, "authenticated-user", "User", UserRole.SUPPORT_ENGINEER);
        when(userRepository.findByUsername("authenticated-user")).thenReturn(Optional.of(user));
        when(taskRepository.findById(19L)).thenReturn(Optional.of(new Task()));
        when(mentionRepository.countForEntityAndMentionedUser("TASK", 19L, 22L)).thenReturn(1L);
        when(commentRepository.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(101L);
            return comment;
        });

        assertDoesNotThrow(() -> commentService.createComment(request(19L, "Another reply", List.of())));
        verify(commentRepository).insert(any(Comment.class));
    }

    @Test
    void adminCanPostTaskCommentWithoutMention() {
        AppUser admin = user(33L, "authenticated-user", "Admin", UserRole.ADMIN);
        when(userRepository.findByUsername("authenticated-user")).thenReturn(Optional.of(admin));
        when(taskRepository.findById(19L)).thenReturn(Optional.of(new Task()));
        when(commentRepository.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(102L);
            return comment;
        });

        assertDoesNotThrow(() -> commentService.createComment(request(19L, "Admin reply", List.of())));
        verify(commentRepository).insert(any(Comment.class));
        verifyNoInteractions(mentionRepository);
    }

    @Test
    void unmentionedUserCanReadTaskComments() {
        when(taskRepository.findById(19L)).thenReturn(Optional.of(new Task()));

        assertDoesNotThrow(() -> permissionService.requireRead(
                EntityType.TASK, 19L, user(22L, "user", "User", UserRole.VIEWER)));
    }

    @Test
    void ticketCreatorCannotCommentWithoutMention() {
        AppUser creator = user(11L, "creator", "Creator", UserRole.SUPPORT_ENGINEER);
        when(ticketRepository.findById(70L)).thenReturn(Optional.of(ticket(70L, 11L)));
        when(mentionRepository.countForEntityAndMentionedUser("TICKET", 70L, 11L)).thenReturn(0L);

        assertFalse(commentService.canComment(70L, creator));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> commentService.createTicketComment(70L, "A reply", creator))
                .getStatusCode().value());
        verify(commentRepository, never()).insert(any());
    }

    @Test
    void adminCanCommentWithoutMention() {
        AppUser admin = user(33L, "admin", "Admin", UserRole.ADMIN);
        when(ticketRepository.findById(70L)).thenReturn(Optional.of(ticket(70L, 11L)));
        when(commentRepository.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(101L);
            return comment;
        });

        assertDoesNotThrow(() -> commentService.createTicketComment(70L, "Admin reply", admin));
        verify(commentRepository).insert(any(Comment.class));
        verifyNoInteractions(mentionRepository);
    }

    @Test
    void namedTicketCommentersCanPostWithoutMention() {
        for (String username : List.of("appadmin", "wasiya")) {
            AppUser user = user(40L, username, username, UserRole.SUPPORT_ENGINEER);
            when(ticketRepository.findById(70L)).thenReturn(Optional.of(ticket(70L, 11L)));
            when(commentRepository.insert(any(Comment.class))).thenAnswer(invocation -> {
                Comment comment = invocation.getArgument(0);
                comment.setId(102L);
                return comment;
            });

            assertTrue(commentService.canComment(70L, user));
            assertDoesNotThrow(() -> commentService.createTicketComment(70L, "Allowed comment", user));
        }
        verify(commentRepository, times(2)).insert(any(Comment.class));
        verifyNoInteractions(mentionRepository);
    }

    @Test
    void ticketMentionIsStoredWithItsTicketId() {
        AppUser author = user(11L, "author", "Author", UserRole.ADMIN);
        AppUser mentioned = user(22L, "bilal", "Bilal", UserRole.FIELD_PERSON);
        when(ticketRepository.findById(70L)).thenReturn(Optional.of(ticket(70L, 11L)));
        when(userRepository.findAllById(any())).thenReturn(List.of(mentioned));
        when(commentRepository.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(100L);
            return comment;
        });

        commentService.createTicketComment(70L, "Hi @bilal", List.of(22L), author);

        verify(mentionRepository).save(argThat(mention ->
                mention.getCommentId().equals(100L)
                        && mention.getMentionedUserId().equals(22L)));
    }

    @Test
    void ticketCommentParsesAndStoresUsernameMentions() {
        AppUser author = user(11L, "creator", "Creator", UserRole.SUPPORT_ENGINEER);
        AppUser mentioned = user(22L, "bilal", "Bilal", UserRole.FIELD_PERSON);
        when(ticketRepository.findById(70L)).thenReturn(Optional.of(ticket(70L, 11L)));
        when(mentionRepository.countForEntityAndMentionedUser("TICKET", 70L, 11L)).thenReturn(1L);
        when(userRepository.findByUsername("bilal")).thenReturn(Optional.of(mentioned));
        when(commentRepository.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(100L);
            return comment;
        });

        CommentDto result = commentService.createTicketComment(70L, "Hi @bilal and @bilal", author);

        assertEquals(List.of(22L), result.mentionedUserIds());
        verify(mentionRepository).save(argThat(mention ->
                mention.getCommentId().equals(100L) && mention.getMentionedUserId().equals(22L)));
    }

    private CreateCommentRequest request(Long taskId, String body, List<Long> mentionedUserIds) {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setEntityType(EntityType.TASK);
        request.setEntityId(taskId);
        request.setBody(body);
        request.setMentionedUserIds(mentionedUserIds);
        return request;
    }

    private AppUser user(Long id, String username, String fullName, UserRole role) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setFullName(fullName);
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private Ticket ticket(Long ticketId, Long creatorId) {
        return com.inventory.msp.incident.model.Ticket.builder()
                .id(ticketId)
                .raisedByUser(user(creatorId, "creator", "Creator", UserRole.SUPPORT_ENGINEER))
                .build();
    }
}
