package commerce.novamart.review.repository;

import commerce.novamart.review.entity.ProductQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<ProductQuestion, UUID> {
    List<ProductQuestion> findByProductIdOrderByCreatedAtDesc(UUID productId);
}
