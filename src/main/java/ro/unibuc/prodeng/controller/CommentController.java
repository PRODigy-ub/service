package ro.unibuc.prodeng.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.unibuc.prodeng.request.CreateCommentRequest;
import ro.unibuc.prodeng.response.CommentResponse;
import ro.unibuc.prodeng.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<CommentResponse> addComment(@Valid @RequestBody CreateCommentRequest request) {
        CommentResponse response = commentService.addComment(
                request.ticketId(),
                request.userId(),
                request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<CommentResponse>> getCommentsByTicketId(@PathVariable String ticketId) {
        List<CommentResponse> responses = commentService.getCommentsByTicket(ticketId);
        return ResponseEntity.ok(responses);
    }
}
