package commerce.novamart.review.repository;

import commerce.novamart.review.entity.QuestionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AnswerRepository extends JpaRepository<QuestionAnswer, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("update QuestionAnswer a set a.helpfulCount = a.helpfulCount + 1 where a.id = :id")
    int incrementHelpful(@Param("id") UUID id);
}
