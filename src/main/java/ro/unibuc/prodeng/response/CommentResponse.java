package ro.unibuc.prodeng.response;

public record CommentResponse(
        String id,
        String content,
        String ticketId,
        String userId) {
}
