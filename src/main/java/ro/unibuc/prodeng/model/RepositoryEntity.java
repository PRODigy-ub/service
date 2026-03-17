package ro.unibuc.prodeng.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import ro.unibuc.prodeng.model.enums.RepositoryStatusEnum;

import java.time.LocalDateTime;

@Document(collection = "repositories")
public record RepositoryEntity(
        @Id String id,
        String name,
        String description,
        String key,
        String ownerId,
        String currentVersion, // (Major.Minor.Patch)
        RepositoryStatusEnum status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
