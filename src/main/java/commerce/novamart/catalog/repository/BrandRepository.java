package commerce.novamart.catalog.repository;

import commerce.novamart.catalog.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {
    Optional<Brand> findBySlug(String slug);

    List<Brand> findByActiveTrueOrderByNameAsc();

    boolean existsBySlug(String slug);
}
