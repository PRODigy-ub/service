package ro.unibuc.prodeng.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "repository_updates")
public record RepositoryUpdateEntity(
        @Id String id,
        String repositoryId,
        String previousVersion,
        String newVersion,
        String updatedById,
        String releaseNotes,
        LocalDateTime updateDate) {
}
