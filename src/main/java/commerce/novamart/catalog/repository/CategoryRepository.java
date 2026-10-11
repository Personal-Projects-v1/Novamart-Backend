package commerce.novamart.catalog.repository;

import commerce.novamart.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlug(String slug);

    List<Category> findByActiveTrueOrderByCreatedAtAscNameAsc();

    boolean existsBySlug(String slug);
}
