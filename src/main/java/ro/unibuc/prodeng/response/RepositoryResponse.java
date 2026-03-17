package ro.unibuc.prodeng.response;

import ro.unibuc.prodeng.model.enums.RepositoryStatusEnum;
import java.time.LocalDateTime;
import java.util.List;

public record RepositoryResponse(
        String id,
        String name,
        String description,
        String key,
        String ownerId,
        String currentVersion,
        RepositoryStatusEnum status,
        long unresolvedTicketsCount,
        List<RepositoryUpdateResponse> versionHistory,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
