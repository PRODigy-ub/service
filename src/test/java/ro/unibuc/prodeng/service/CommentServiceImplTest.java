package ro.unibuc.prodeng.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.unibuc.prodeng.exception.InvalidOperationException;
import ro.unibuc.prodeng.exception.TicketNotFoundException;
import ro.unibuc.prodeng.model.CommentEntity;
import ro.unibuc.prodeng.model.TicketEntity;
import ro.unibuc.prodeng.model.enums.TicketStatusEnum;
import ro.unibuc.prodeng.repository.CommentRepository;
import ro.unibuc.prodeng.repository.TicketRepository;
import ro.unibuc.prodeng.response.CommentResponse;
import ro.unibuc.prodeng.service.impl.CommentServiceImpl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    private TicketEntity ticket;
    private CommentEntity comment;

    @BeforeEach
    void setUp() {
        ticket = new TicketEntity("1", "Test Ticket", "A test ticket", "1", "user1", null,
                TicketStatusEnum.OPEN, "1.0.0");
        comment = new CommentEntity("1", "Test comment", "1", "user1");
    }

    @Test
    void testAddComment() {
        when(ticketRepository.findById("1")).thenReturn(Optional.of(ticket));
        when(commentRepository.save(any(CommentEntity.class))).thenReturn(comment);

        CommentResponse response = commentService.addComment("1", "user1", "Test comment");

        assertNotNull(response);
        assertEquals("Test comment", response.content());
    }

    @Test
    void testAddComment_TicketNotFound() {
        when(ticketRepository.findById("1")).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () ->
                commentService.addComment("1", "user1", "Test comment"));
    }

    @Test
    void testAddComment_TicketClosed() {
        TicketEntity closedTicket = new TicketEntity("1", "Test Ticket", "A test ticket", "1", "user1", null,
                TicketStatusEnum.CLOSED, "1.0.0");
        when(ticketRepository.findById("1")).thenReturn(Optional.of(closedTicket));

        assertThrows(InvalidOperationException.class, () ->
                commentService.addComment("1", "user1", "Test comment"));
    }

    @Test
    void testGetCommentsByTicket() {
        when(ticketRepository.existsById("1")).thenReturn(true);
        when(commentRepository.findByTicketId("1")).thenReturn(Collections.singletonList(comment));

        List<CommentResponse> responses = commentService.getCommentsByTicket("1");

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        assertEquals("Test comment", responses.get(0).content());
    }

    @Test
    void testGetCommentsByTicket_TicketNotFound() {
        when(ticketRepository.existsById("1")).thenReturn(false);

        assertThrows(TicketNotFoundException.class, () -> commentService.getCommentsByTicket("1"));
    }
}
