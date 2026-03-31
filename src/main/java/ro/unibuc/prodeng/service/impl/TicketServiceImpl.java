package ro.unibuc.prodeng.service.impl;

import org.springframework.stereotype.Service;
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
import ro.unibuc.prodeng.service.TicketService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final RepositoryRepository repositoryRepository;
    private final CommentRepository commentRepository;

    public TicketServiceImpl(TicketRepository ticketRepository, RepositoryRepository repositoryRepository,
            CommentRepository commentRepository) {
        this.ticketRepository = ticketRepository;
        this.repositoryRepository = repositoryRepository;
        this.commentRepository = commentRepository;
    }

    @Override
    public TicketDetailResponse createTicket(String title, String description, String repositoryId,
            String assignedById) {
        RepositoryEntity repo = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));

        if (repo.status() == RepositoryStatusEnum.MAINTENANCE) {
            throw new InvalidOperationException("Cannot create tickets on a repository in MAINTENANCE.");
        }

        TicketEntity ticket = new TicketEntity(
                null, title, description, repositoryId, assignedById,
                null,
                TicketStatusEnum.OPEN,
                repo.currentVersion());

        TicketEntity savedTicket = ticketRepository.save(ticket);
        return mapToResponse(savedTicket);
    }

    @Override
    public TicketDetailResponse changeStatus(String ticketId, String newStatusEnumName) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        TicketStatusEnum newStatus = TicketStatusEnum.valueOf(newStatusEnumName.toUpperCase());

        if (ticket.status() == TicketStatusEnum.CLOSED) {
            throw new InvalidOperationException("Cannot change status of a CLOSED ticket.");
        }

        TicketEntity updatedTicket = new TicketEntity(
                ticket.id(), ticket.title(), ticket.description(), ticket.repositoryId(),
                ticket.assignedById(), ticket.assignedToId(), newStatus, ticket.targetVersion());

        TicketEntity savedTicket = ticketRepository.save(updatedTicket);
        return mapToResponse(savedTicket);
    }

    @Override
    public TicketDetailResponse assignTicket(String ticketId, String currentUserId, String assignedToId) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        RepositoryEntity repo = repositoryRepository.findById(ticket.repositoryId())
                .orElseThrow(() -> new RepositoryNotFoundException(ticket.repositoryId()));

        if (repo.status() == RepositoryStatusEnum.MAINTENANCE) {
            throw new InvalidOperationException("Cannot reassign tickets for a repository in MAINTENANCE.");
        }

        if (ticket.status() == TicketStatusEnum.CLOSED || ticket.status() == TicketStatusEnum.RESOLVED) {
            throw new InvalidOperationException("Cannot reassign a resolved or closed ticket.");
        }

        // Complex check: only the owner or the current assigner or the current assignee can reassign
        boolean isOwner = repo.ownerId().equals(currentUserId);
        boolean isAssigner = ticket.assignedById().equals(currentUserId);
        boolean isAssignee = ticket.assignedToId() != null && ticket.assignedToId().equals(currentUserId);

        if (!isOwner && !isAssigner && !isAssignee) {
            throw new InvalidOperationException("Unauthorized to reassign this ticket.");
        }

        TicketEntity updatedTicket = new TicketEntity(
                ticket.id(), ticket.title(), ticket.description(), ticket.repositoryId(),
                ticket.assignedById(), assignedToId, TicketStatusEnum.IN_EXECUTION, ticket.targetVersion());

        TicketEntity savedTicket = ticketRepository.save(updatedTicket);
        return mapToResponse(savedTicket);
    }

    @Override
    public TicketDetailResponse resolveTicket(String ticketId, String userId) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (ticket.status() != TicketStatusEnum.IN_EXECUTION) {
            throw new InvalidOperationException("Ticket must be IN_EXECUTION to be resolved.");
        }

        if (ticket.assignedToId() == null || !ticket.assignedToId().equals(userId)) {
            throw new InvalidOperationException("Only the assignee can resolve the ticket.");
        }

        long commentCount = commentRepository.countByTicketId(ticketId);
        if (commentCount == 0) {
            throw new InvalidOperationException("Ticket cannot be resolved without at least one comment explaining the solution.");
        }

        TicketEntity updatedTicket = new TicketEntity(
                ticket.id(), ticket.title(), ticket.description(), ticket.repositoryId(),
                ticket.assignedById(), ticket.assignedToId(), TicketStatusEnum.RESOLVED, ticket.targetVersion());

        TicketEntity savedTicket = ticketRepository.save(updatedTicket);
        return mapToResponse(savedTicket);
    }

    @Override
    public List<TicketDetailResponse> getTicketsByRepository(String repositoryId) {
        if (!repositoryRepository.existsById(repositoryId)) {
            throw new RepositoryNotFoundException(repositoryId);
        }
        return ticketRepository.findByRepositoryId(repositoryId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TicketDetailResponse mapToResponse(TicketEntity ticket) {
        return new TicketDetailResponse(
                ticket.id(), ticket.title(), ticket.description(), ticket.repositoryId(),
                ticket.assignedById(), ticket.assignedToId(), ticket.status(), ticket.targetVersion());
    }
}
