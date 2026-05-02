package ro.unibuc.prodeng.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ro.unibuc.prodeng.model.TodoEntity;

@Repository
public interface TodoRepository extends MongoRepository<TodoEntity, String> {

    List<TodoEntity> findByAssignedUserId(String assignedUserId);

    long countByDoneFalse();
}
