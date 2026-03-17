package ro.unibuc.prodeng.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "comments")
public record CommentEntity(
        @Id String id,
        String content,
        String ticketId,
        String userId) {
}
