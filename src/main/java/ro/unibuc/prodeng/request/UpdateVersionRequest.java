package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateVersionRequest(
        @NotBlank(message = "New version is required") @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "Version must be in Major.Minor.Patch format") String newVersion,

        String releaseNotes,

        @NotBlank(message = "Updater user ID is required") String updatedById) {
}
