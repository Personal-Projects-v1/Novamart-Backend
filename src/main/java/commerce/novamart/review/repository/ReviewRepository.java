package commerce.novamart.review.repository;

import commerce.novamart.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query("select r from Review r join fetch r.product where r.product.id = :productId and r.status = 'APPROVED' order by r.createdAt desc")
    List<Review> findApprovedForProduct(@Param("productId") UUID productId);

    @Query("select r from Review r join fetch r.product where r.user.id = :userId order by r.createdAt desc")
    List<Review> findByUser(@Param("userId") UUID userId);

    boolean existsByProductIdAndUserId(UUID productId, UUID userId);

    Page<Review> findByStatus(Review.Status status, Pageable pageable);

    @Query("select avg(r.rating), count(r) from Review r where r.product.id = :productId and r.status = 'APPROVED'")
    List<Object[]> aggregate(@Param("productId") UUID productId);

    @Modifying(clearAutomatically = true)
    @Query("update Review r set r.helpfulCount = r.helpfulCount + 1 where r.id = :id")
    int incrementHelpful(@Param("id") UUID id);
}
