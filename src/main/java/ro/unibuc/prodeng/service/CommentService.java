package ro.unibuc.prodeng.service;

import ro.unibuc.prodeng.response.CommentResponse;
import java.util.List;

public interface CommentService {
    CommentResponse addComment(String ticketId, String userId, String content);

    List<CommentResponse> getCommentsByTicket(String ticketId);
}
