package ro.unibuc.prodeng.response;

import ro.unibuc.prodeng.model.enums.TicketStatusEnum;

public record TicketDetailResponse(
        String id,
        String title,
        String description,
        String repositoryId,
        String assignedById,
        String assignedToId,
        TicketStatusEnum status,
        String targetVersion) {
}
