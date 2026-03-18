package ro.unibuc.prodeng.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ro.unibuc.prodeng.model.TicketEntity;
import ro.unibuc.prodeng.model.enums.TicketStatusEnum;

import java.util.List;

@Repository
public interface TicketRepository extends MongoRepository<TicketEntity, String> {

    List<TicketEntity> findByRepositoryId(String repositoryId);

    boolean existsByRepositoryIdAndTargetVersionAndStatusNot(String repositoryId, String targetVersion,
            TicketStatusEnum status);

    long countByRepositoryIdAndStatusNot(String repositoryId, TicketStatusEnum status);
}
