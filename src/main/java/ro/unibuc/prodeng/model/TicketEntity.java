package ro.unibuc.prodeng.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import ro.unibuc.prodeng.model.enums.TicketStatusEnum;

@Document(collection = "tickets")
public record TicketEntity(
        @Id String id,
        String title,
        String description,
        String repositoryId,
        String assignedById,
        String assignedToId,
        TicketStatusEnum status,
        String targetVersion
) {
}
