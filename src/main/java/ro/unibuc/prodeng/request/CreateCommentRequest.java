package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
        @NotBlank(message = "Ticket ID is required") String ticketId,
        @NotBlank(message = "User ID is required") String userId,
        @NotBlank(message = "Content is required") String content) {
}
