package ro.unibuc.prodeng.service.impl;

import org.springframework.stereotype.Service;
import ro.unibuc.prodeng.exception.InvalidOperationException;
import ro.unibuc.prodeng.exception.RepositoryNotFoundException;
import ro.unibuc.prodeng.model.RepositoryEntity;
import ro.unibuc.prodeng.model.RepositoryUpdateEntity;
import ro.unibuc.prodeng.model.enums.RepositoryStatusEnum;
import ro.unibuc.prodeng.model.enums.TicketStatusEnum;
import ro.unibuc.prodeng.repository.RepositoryRepository;
import ro.unibuc.prodeng.repository.RepositoryUpdateRepository;
import ro.unibuc.prodeng.repository.TicketRepository;
import ro.unibuc.prodeng.response.RepositoryResponse;
import ro.unibuc.prodeng.response.RepositoryUpdateResponse;
import ro.unibuc.prodeng.service.RepositoryService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RepositoryServiceImpl implements RepositoryService {

    private final RepositoryRepository repositoryRepository;
    private final RepositoryUpdateRepository updateRepository;
    private final TicketRepository ticketRepository;

    public RepositoryServiceImpl(RepositoryRepository repositoryRepository,
            RepositoryUpdateRepository updateRepository,
            TicketRepository ticketRepository) {
        this.repositoryRepository = repositoryRepository;
        this.updateRepository = updateRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public RepositoryResponse createRepository(String name, String description, String key, String ownerId) {
        if (repositoryRepository.existsByKey(key)) {
            throw new InvalidOperationException("Repository key already exists.");
        }

        RepositoryEntity repo = new RepositoryEntity(
                null, name, description, key, ownerId,
                "1.0.0",
                RepositoryStatusEnum.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());

        RepositoryEntity savedRepo = repositoryRepository.save(repo);
        return mapToResponse(savedRepo);
    }

    @Override
    public RepositoryResponse updateVersion(String repositoryId, String newVersion, String releaseNotes,
            String updatedById) {
        RepositoryEntity repo = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));

        if (repo.status() != RepositoryStatusEnum.ACTIVE) {
            throw new InvalidOperationException("Cannot update version. Repository is not ACTIVE.");
        }

        String currentVersion = repo.currentVersion();
        String[] currentParts = currentVersion.split("\\.");
        String[] newParts = newVersion.split("\\.");

        if (currentParts.length == 3 && newParts.length == 3) {
            int currentMajor = Integer.parseInt(currentParts[0]);
            int newMajor = Integer.parseInt(newParts[0]);
            int currentMinor = Integer.parseInt(currentParts[1]);
            int newMinor = Integer.parseInt(newParts[1]);

            if (newMajor > currentMajor) {
                boolean hasAnyUnclosedTickets = ticketRepository.countByRepositoryIdAndStatusNot(repositoryId,
                        TicketStatusEnum.CLOSED) > 0;
                if (hasAnyUnclosedTickets) {
                    throw new InvalidOperationException(
                            "Cannot perform MAJOR update. There are pending unclosed tickets.");
                }
            } else if (newMinor > currentMinor && newMajor == currentMajor) {
                boolean hasUnclosedOnCurrentVersion = ticketRepository.existsByRepositoryIdAndTargetVersionAndStatusNot(
                        repositoryId, currentVersion, TicketStatusEnum.CLOSED);
                if (hasUnclosedOnCurrentVersion) {
                    throw new InvalidOperationException(
                            "Cannot perform MINOR update. Close tickets on version " + currentVersion + " first.");
                }
            }
        }

        RepositoryEntity updatedRepo = new RepositoryEntity(
                repo.id(), repo.name(), repo.description(), repo.key(), repo.ownerId(),
                newVersion, repo.status(), repo.createdAt(), LocalDateTime.now());
        updatedRepo = repositoryRepository.save(updatedRepo);

        RepositoryUpdateEntity updateLog = new RepositoryUpdateEntity(
                null, repo.id(), currentVersion, newVersion, updatedById, releaseNotes, LocalDateTime.now());
        updateRepository.save(updateLog);

        return mapToResponse(updatedRepo);
    }

    @Override
    public RepositoryResponse getRepositoryDetails(String repositoryId) {
        RepositoryEntity repo = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
        return mapToResponse(repo);
    }

    private RepositoryResponse mapToResponse(RepositoryEntity repo) {
        long unresolvedCount = ticketRepository.countByRepositoryIdAndStatusNot(repo.id(), TicketStatusEnum.CLOSED);

        List<RepositoryUpdateResponse> history = updateRepository.findByRepositoryIdOrderByUpdateDateDesc(repo.id())
                .stream()
                .map(u -> new RepositoryUpdateResponse(u.previousVersion(), u.newVersion(), u.releaseNotes(),
                        u.updateDate()))
                .collect(Collectors.toList());

        return new RepositoryResponse(
                repo.id(), repo.name(), repo.description(), repo.key(), repo.ownerId(),
                repo.currentVersion(), repo.status(), unresolvedCount, history, repo.createdAt(), repo.updatedAt());
    }
}
