package ro.unibuc.prodeng.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ro.unibuc.prodeng.model.CommentEntity;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<CommentEntity, String> {

    List<CommentEntity> findByTicketId(String ticketId);
}
