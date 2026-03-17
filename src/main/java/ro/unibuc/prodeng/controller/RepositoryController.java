package ro.unibuc.prodeng.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.unibuc.prodeng.request.CreateRepositoryRequest;
import ro.unibuc.prodeng.request.UpdateVersionRequest;
import ro.unibuc.prodeng.response.RepositoryResponse;
import ro.unibuc.prodeng.service.RepositoryService;

@RestController
@RequestMapping("/api/repositories")
public class RepositoryController {

    private final RepositoryService repositoryService;

    public RepositoryController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @PostMapping
    public ResponseEntity<RepositoryResponse> createRepository(@Valid @RequestBody CreateRepositoryRequest request) {
        RepositoryResponse response = repositoryService.createRepository(
                request.name(),
                request.description(),
                request.key(),
                request.ownerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/version")
    public ResponseEntity<RepositoryResponse> updateVersion(
            @PathVariable String id,
            @Valid @RequestBody UpdateVersionRequest request) {

        RepositoryResponse response = repositoryService.updateVersion(
                id,
                request.newVersion(),
                request.releaseNotes(),
                request.updatedById());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepositoryResponse> getRepository(@PathVariable String id) {
        RepositoryResponse response = repositoryService.getRepositoryDetails(id);
        return ResponseEntity.ok(response);
    }
}
