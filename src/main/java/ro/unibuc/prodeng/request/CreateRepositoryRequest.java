package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.NotBlank;

public record CreateRepositoryRequest(
        @NotBlank(message = "Name is required") String name,
        String description,
        @NotBlank(message = "Key is required") String key,
        @NotBlank(message = "Owner ID is required") String ownerId) {
}
