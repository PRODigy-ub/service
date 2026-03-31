package ro.unibuc.prodeng.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.unibuc.prodeng.exception.InvalidOperationException;
import ro.unibuc.prodeng.exception.RepositoryNotFoundException;
import ro.unibuc.prodeng.exception.TicketNotFoundException;
import ro.unibuc.prodeng.model.RepositoryEntity;
import ro.unibuc.prodeng.model.TicketEntity;
import ro.unibuc.prodeng.model.enums.RepositoryStatusEnum;
import ro.unibuc.prodeng.model.enums.TicketStatusEnum;
import ro.unibuc.prodeng.repository.CommentRepository;
import ro.unibuc.prodeng.repository.RepositoryRepository;
import ro.unibuc.prodeng.repository.TicketRepository;
import ro.unibuc.prodeng.response.TicketDetailResponse;
import ro.unibuc.prodeng.service.impl.TicketServiceImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private RepositoryEntity repository;
    private TicketEntity ticket;

    @BeforeEach
    void setUp() {
        repository = new RepositoryEntity("1", "Test Repo", "A test repository", "TEST", "user1", "1.0.0",
                RepositoryStatusEnum.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        ticket = new TicketEntity("1", "Test Ticket", "A test ticket", "1", "user1", null,
                TicketStatusEnum.OPEN, "1.0.0");
    }

    @Test
    void testCreateTicket() {
        when(repositoryRepository.findById("1")).thenReturn(Optional.of(repository));
        when(ticketRepository.save(any(TicketEntity.class))).thenReturn(ticket);

        TicketDetailResponse response = ticketService.createTicket("Test Ticket", "A test ticket", "1", "user1");

        assertNotNull(response);
        assertEquals("Test Ticket", response.title());
    }

    @Test
    void testCreateTicket_RepositoryNotFound() {
        when(repositoryRepository.findById("1")).thenReturn(Optional.empty());

        assertThrows(RepositoryNotFoundException.class, () ->
                ticketService.createTicket("Test Ticket", "A test ticket", "1", "user1"));
    }

    @Test
    void testCreateTicket_RepositoryInMaintenance() {
        repository = new RepositoryEntity("1", "Test Repo", "A test repository", "TEST", "user1", "1.0.0",
                RepositoryStatusEnum.MAINTENANCE, LocalDateTime.now(), LocalDateTime.now());
        when(repositoryRepository.findById("1")).thenReturn(Optional.of(repository));

        assertThrows(InvalidOperationException.class, () ->
                ticketService.createTicket("Test Ticket", "A test ticket", "1", "user1"));
    }

    @Test
    void testChangeStatus() {
        TicketEntity inProgressTicket = new TicketEntity("1", "Test Ticket", "A test ticket", "1", "user1", null,
                TicketStatusEnum.IN_EXECUTION, "1.0.0");
        when(ticketRepository.findById("1")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(TicketEntity.class))).thenReturn(inProgressTicket);

        TicketDetailResponse response = ticketService.changeStatus("1", "IN_EXECUTION");

        assertNotNull(response);
        assertEquals(TicketStatusEnum.IN_EXECUTION, response.status());
    }

    @Test
    void testChangeStatus_TicketNotFound() {
        when(ticketRepository.findById("1")).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketService.changeStatus("1", "IN_EXECUTION"));
    }

    @Test
    void testChangeStatus_TicketClosed() {
        TicketEntity closedTicket = new TicketEntity("1", "Test Ticket", "A test ticket", "1", "user1", null,
                TicketStatusEnum.CLOSED, "1.0.0");
        when(ticketRepository.findById("1")).thenReturn(Optional.of(closedTicket));

        assertThrows(InvalidOperationException.class, () -> ticketService.changeStatus("1", "IN_EXECUTION"));
    }

    @Test
    void testGetTicketsByRepository() {
        when(repositoryRepository.existsById("1")).thenReturn(true);
        when(ticketRepository.findByRepositoryId("1")).thenReturn(Collections.singletonList(ticket));

        List<TicketDetailResponse> responses = ticketService.getTicketsByRepository("1");

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        assertEquals("Test Ticket", responses.get(0).title());
    }

    @Test
    void testGetTicketsByRepository_RepositoryNotFound() {
        when(repositoryRepository.existsById("1")).thenReturn(false);

        assertThrows(RepositoryNotFoundException.class, () -> ticketService.getTicketsByRepository("1"));
    }

    @ParameterizedTest
    @MethodSource("provideResolveTicketFailureCases")
    void testResolveTicket_Failures(TicketStatusEnum status, String assigneeId, long commentCount, String expectedMessage) {
        // Arrange
        TicketEntity ticketToResolve = new TicketEntity("1", "Title", "Desc", "repo1", "user1", assigneeId, status, "1.0.0");
        when(ticketRepository.findById("1")).thenReturn(Optional.of(ticketToResolve));
        
        if (status == TicketStatusEnum.IN_EXECUTION && "assignee1".equals(assigneeId)) {
            when(commentRepository.countByTicketId("1")).thenReturn(commentCount);
        }

        // Act & Assert
        InvalidOperationException exception = assertThrows(InvalidOperationException.class, () -> 
            ticketService.resolveTicket("1", "assignee1")
        );
        assertEquals(expectedMessage, exception.getMessage());
    }

    private static Stream<Arguments> provideResolveTicketFailureCases() {
        return Stream.of(
            Arguments.of(TicketStatusEnum.OPEN, "assignee1", 1L, "Ticket must be IN_EXECUTION to be resolved."),
            Arguments.of(TicketStatusEnum.IN_EXECUTION, "otherUser", 1L, "Only the assignee can resolve the ticket."),
            Arguments.of(TicketStatusEnum.IN_EXECUTION, "assignee1", 0L, "Ticket cannot be resolved without at least one comment explaining the solution.")
        );
    }

    @Test
    void testResolveTicket_Success() {
        // Arrange
        TicketEntity ticketInExecution = new TicketEntity("1", "Title", "Desc", "repo1", "user1", "assignee1", TicketStatusEnum.IN_EXECUTION, "1.0.0");
        when(ticketRepository.findById("1")).thenReturn(Optional.of(ticketInExecution));
        when(commentRepository.countByTicketId("1")).thenReturn(1L);
        when(ticketRepository.save(any(TicketEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        TicketDetailResponse response = ticketService.resolveTicket("1", "assignee1");

        // Assert
        assertEquals(TicketStatusEnum.RESOLVED, response.status());
    }
}
