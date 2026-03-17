package ro.unibuc.prodeng.response;

import java.time.LocalDateTime;

public record RepositoryUpdateResponse(
        String previousVersion,
        String newVersion,
        String releaseNotes,
        LocalDateTime updateDate) {
}
