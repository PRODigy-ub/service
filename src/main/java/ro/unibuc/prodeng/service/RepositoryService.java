package ro.unibuc.prodeng.service;

import ro.unibuc.prodeng.response.RepositoryResponse;

public interface RepositoryService {
    RepositoryResponse createRepository(String name, String description, String key, String ownerId);

    RepositoryResponse updateVersion(String repositoryId, String newVersion, String releaseNotes, String updatedById);

    RepositoryResponse getRepositoryDetails(String repositoryId);
}
