package commerce.novamart.catalog.repository;

import commerce.novamart.catalog.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlugAndActiveTrue(String slug);

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySku(String sku);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    List<Product> findByIdInAndActiveTrue(Collection<UUID> ids);

    @Query("select p from Product p where p.active = true and p.featured = true order by p.rating desc")
    List<Product> findFeatured(Pageable pageable);

    @Query("select p from Product p where p.active = true order by p.bestSeller desc, p.reviewCount desc")
    List<Product> findBestSellers(Pageable pageable);

    @Query("select p from Product p where p.active = true order by p.createdAt desc")
    List<Product> findNewest(Pageable pageable);

    @Query("select p from Product p where p.active = true order by p.reviewCount desc")
    List<Product> findTrending(Pageable pageable);

    @Query("select p from Product p where p.active = true order by p.rating desc, p.reviewCount desc")
    List<Product> findTopRated(Pageable pageable);

    @Query("""
            select p from Product p
            where p.active = true and p.id <> :id and (p.category.id = :categoryId or p.brand.id = :brandId)
            order by case when p.category.id = :categoryId then 0 else 1 end, p.rating desc
            """)
    List<Product> findRelated(@Param("id") UUID id, @Param("categoryId") UUID categoryId,
                              @Param("brandId") UUID brandId, Pageable pageable);

    @Query("select p.brand.id, count(p) from Product p where p.active = true group by p.brand.id")
    List<Object[]> countByBrand();

    @Query("select p.category.id, count(p) from Product p where p.active = true group by p.category.id")
    List<Object[]> countByCategory();

    long countByBrandId(UUID brandId);

    long countByCategoryId(UUID categoryId);

    /** Atomically takes stock; returns 0 when there is not enough. */
    @Modifying(flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :qty where p.id = :id and p.stock >= :qty")
    int decrementStock(@Param("id") UUID id, @Param("qty") int qty);

    @Modifying(flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock + :qty where p.id = :id")
    int incrementStock(@Param("id") UUID id, @Param("qty") int qty);

    long countByStockLessThanEqualAndActiveTrue(int stock);
}
