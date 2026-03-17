package ro.unibuc.prodeng.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ro.unibuc.prodeng.model.RepositoryUpdateEntity;

import java.util.List;

@Repository
public interface RepositoryUpdateRepository extends MongoRepository<RepositoryUpdateEntity, String> {

    List<RepositoryUpdateEntity> findByRepositoryIdOrderByUpdateDateDesc(String repositoryId);
}
