package commerce.novamart.catalog.repository;

import commerce.novamart.catalog.entity.ProductCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductCollectionRepository extends JpaRepository<ProductCollection, UUID> {
    Optional<ProductCollection> findBySlug(String slug);
}
