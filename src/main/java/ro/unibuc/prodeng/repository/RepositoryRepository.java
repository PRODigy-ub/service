package ro.unibuc.prodeng.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ro.unibuc.prodeng.model.RepositoryEntity;

import java.util.List;

@Repository
public interface RepositoryRepository extends MongoRepository<RepositoryEntity, String> {

    List<RepositoryEntity> findByOwnerId(String ownerId);

    boolean existsByKey(String key);
}
