package ro.unibuc.prodeng.service.impl;

import org.springframework.stereotype.Service;
import ro.unibuc.prodeng.exception.InvalidOperationException;
import ro.unibuc.prodeng.exception.TicketNotFoundException;
import ro.unibuc.prodeng.model.CommentEntity;
import ro.unibuc.prodeng.model.TicketEntity;
import ro.unibuc.prodeng.model.enums.TicketStatusEnum;
import ro.unibuc.prodeng.repository.CommentRepository;
import ro.unibuc.prodeng.repository.TicketRepository;
import ro.unibuc.prodeng.response.CommentResponse;
import ro.unibuc.prodeng.service.CommentService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;

    public CommentServiceImpl(CommentRepository commentRepository, TicketRepository ticketRepository) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public CommentResponse addComment(String ticketId, String userId, String content) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (ticket.status() == TicketStatusEnum.CLOSED) {
            throw new InvalidOperationException("Cannot add comments to a closed ticket.");
        }

        CommentEntity comment = new CommentEntity(
                null, content, ticketId, userId);

        CommentEntity savedComment = commentRepository.save(comment);
        return mapToResponse(savedComment);
    }

    @Override
    public List<CommentResponse> getCommentsByTicket(String ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new TicketNotFoundException(ticketId);
        }
        return commentRepository.findByTicketId(ticketId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private CommentResponse mapToResponse(CommentEntity comment) {
        return new CommentResponse(
                comment.id(), comment.content(), comment.ticketId(), comment.userId());
    }
}
