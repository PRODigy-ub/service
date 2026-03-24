package ro.unibuc.prodeng.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.unibuc.prodeng.exception.InvalidOperationException;
import ro.unibuc.prodeng.exception.RepositoryNotFoundException;
import ro.unibuc.prodeng.model.RepositoryEntity;
import ro.unibuc.prodeng.model.RepositoryUpdateEntity;
import ro.unibuc.prodeng.model.enums.RepositoryStatusEnum;
import ro.unibuc.prodeng.model.enums.TicketStatusEnum;
import ro.unibuc.prodeng.repository.RepositoryRepository;
import ro.unibuc.prodeng.repository.RepositoryUpdateRepository;
import ro.unibuc.prodeng.repository.TicketRepository;
import ro.unibuc.prodeng.response.RepositoryResponse;
import ro.unibuc.prodeng.service.impl.RepositoryServiceImpl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryServiceImplTest {

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private RepositoryUpdateRepository updateRepository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private RepositoryServiceImpl repositoryService;

    private RepositoryEntity repository;

    @BeforeEach
    void setUp() {
        repository = new RepositoryEntity("1", "Test Repo", "A test repository", "TEST", "user1", "1.0.0",
                RepositoryStatusEnum.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void testCreateRepository() {
        when(repositoryRepository.existsByKey("TEST")).thenReturn(false);
        when(repositoryRepository.save(any(RepositoryEntity.class))).thenReturn(repository);

        RepositoryResponse response = repositoryService.createRepository("Test Repo", "A test repository", "TEST", "user1");

        assertNotNull(response);
        assertEquals("Test Repo", response.name());
    }

    @Test
    void testCreateRepository_KeyAlreadyExists() {
        when(repositoryRepository.existsByKey("TEST")).thenReturn(true);

        assertThrows(InvalidOperationException.class, () ->
                repositoryService.createRepository("Test Repo", "A test repository", "TEST", "user1"));
    }

    @Test
    void testUpdateVersion_Major() {
        RepositoryEntity updatedRepository = new RepositoryEntity("1", "Test Repo", "A test repository", "TEST", "user1", "2.0.0",
                RepositoryStatusEnum.ACTIVE, repository.createdAt(), LocalDateTime.now());
        when(repositoryRepository.findById("1")).thenReturn(Optional.of(repository));
        when(ticketRepository.countByRepositoryIdAndStatusNot("1", TicketStatusEnum.CLOSED)).thenReturn(0L);
        when(repositoryRepository.save(any(RepositoryEntity.class))).thenReturn(updatedRepository);
        when(updateRepository.save(any(RepositoryUpdateEntity.class))).thenReturn(null);

        RepositoryResponse response = repositoryService.updateVersion("1", "2.0.0", "Major update", "user2");

        assertNotNull(response);
        assertEquals("2.0.0", response.currentVersion());
    }

    @Test
    void testUpdateVersion_Minor() {
        RepositoryEntity updatedRepository = new RepositoryEntity("1", "Test Repo", "A test repository", "TEST", "user1", "1.1.0",
                RepositoryStatusEnum.ACTIVE, repository.createdAt(), LocalDateTime.now());
        when(repositoryRepository.findById("1")).thenReturn(Optional.of(repository));
        when(ticketRepository.existsByRepositoryIdAndTargetVersionAndStatusNot("1", "1.0.0", TicketStatusEnum.CLOSED))
                .thenReturn(false);
        when(repositoryRepository.save(any(RepositoryEntity.class))).thenReturn(updatedRepository);
        when(updateRepository.save(any(RepositoryUpdateEntity.class))).thenReturn(null);

        RepositoryResponse response = repositoryService.updateVersion("1", "1.1.0", "Minor update", "user2");

        assertNotNull(response);
        assertEquals("1.1.0", response.currentVersion());
    }

    @Test
    void testUpdateVersion_Patch() {
        RepositoryEntity updatedRepository = new RepositoryEntity("1", "Test Repo", "A test repository", "TEST", "user1", "1.0.1",
                RepositoryStatusEnum.ACTIVE, repository.createdAt(), LocalDateTime.now());
        when(repositoryRepository.findById("1")).thenReturn(Optional.of(repository));
        when(repositoryRepository.save(any(RepositoryEntity.class))).thenReturn(updatedRepository);
        when(updateRepository.save(any(RepositoryUpdateEntity.class))).thenReturn(null);

        RepositoryResponse response = repositoryService.updateVersion("1", "1.0.1", "Patch update", "user2");

        assertNotNull(response);
        assertEquals("1.0.1", response.currentVersion());
    }

    @Test
    void testUpdateVersion_RepositoryNotFound() {
        when(repositoryRepository.findById("1")).thenReturn(Optional.empty());

        assertThrows(RepositoryNotFoundException.class, () ->
                repositoryService.updateVersion("1", "2.0.0", "Major update", "user2"));
    }

    @Test
    void testUpdateVersion_RepositoryNotActive() {
        repository = new RepositoryEntity("1", "Test Repo", "A test repository", "TEST", "user1", "1.0.0",
                RepositoryStatusEnum.ARCHIVED, LocalDateTime.now(), LocalDateTime.now());
        when(repositoryRepository.findById("1")).thenReturn(Optional.of(repository));

        assertThrows(InvalidOperationException.class, () ->
                repositoryService.updateVersion("1", "2.0.0", "Major update", "user2"));
    }

    @Test
    void testUpdateVersion_MajorUpdateWithUnclosedTickets() {
        when(repositoryRepository.findById("1")).thenReturn(Optional.of(repository));
        when(ticketRepository.countByRepositoryIdAndStatusNot("1", TicketStatusEnum.CLOSED)).thenReturn(1L);

        assertThrows(InvalidOperationException.class, () ->
                repositoryService.updateVersion("1", "2.0.0", "Major update", "user2"));
    }

    @Test
    void testUpdateVersion_MinorUpdateWithUnclosedTickets() {
        when(repositoryRepository.findById("1")).thenReturn(Optional.of(repository));
        when(ticketRepository.existsByRepositoryIdAndTargetVersionAndStatusNot("1", "1.0.0", TicketStatusEnum.CLOSED))
                .thenReturn(true);

        assertThrows(InvalidOperationException.class, () ->
                repositoryService.updateVersion("1", "1.1.0", "Minor update", "user2"));
    }

    @Test
    void testGetRepositoryDetails() {
        when(repositoryRepository.findById("1")).thenReturn(Optional.of(repository));
        when(ticketRepository.countByRepositoryIdAndStatusNot("1", TicketStatusEnum.CLOSED)).thenReturn(0L);
        when(updateRepository.findByRepositoryIdOrderByUpdateDateDesc("1")).thenReturn(Collections.emptyList());

        RepositoryResponse response = repositoryService.getRepositoryDetails("1");

        assertNotNull(response);
        assertEquals("Test Repo", response.name());
    }

    @Test
    void testGetRepositoryDetails_NotFound() {
        when(repositoryRepository.findById("1")).thenReturn(Optional.empty());

        assertThrows(RepositoryNotFoundException.class, () -> repositoryService.getRepositoryDetails("1"));
    }
}
