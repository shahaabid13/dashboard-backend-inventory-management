package com.inventory.msp.comments.controller;

import com.inventory.msp.comments.model.EntityType;
import com.inventory.msp.comments.service.CommentService;
import com.inventory.msp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;

class TicketCommentControllerTest {
    private final CommentService commentService = mock(CommentService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new TicketCommentController(commentService, mock(UserRepository.class)))
            .build();

    @Test
    void ticketCommentsCanBeReadFromTicketScopedRoute() throws Exception {
        when(commentService.getComments(EntityType.TICKET, 30L)).thenReturn(List.of());

        mockMvc.perform(get("/api/tickets/30/comments"))
                .andExpect(status().isOk());
    }

    @Test
    void editingOrDeletingCommentsIsNotAllowed() throws Exception {
        mockMvc.perform(put("/api/tickets/1/comments"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(patch("/api/tickets/1/comments"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/tickets/1/comments"))
                .andExpect(status().isMethodNotAllowed());
    }
}
