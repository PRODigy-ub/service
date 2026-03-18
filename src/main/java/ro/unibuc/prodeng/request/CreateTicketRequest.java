package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.NotBlank;

public record CreateTicketRequest(
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "Description is required") String description,
        @NotBlank(message = "Repository ID is required") String repositoryId,
        @NotBlank(message = "Assigned By ID is required") String assignedById) {
}
