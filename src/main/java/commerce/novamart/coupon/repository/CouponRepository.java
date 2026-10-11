package commerce.novamart.coupon.repository;

import commerce.novamart.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    List<Coupon> findByActiveTrueAndExpiresAtAfterOrderByExpiresAtAsc(LocalDateTime now);

    /** Atomic usage bump that respects the usage limit; returns 0 when the coupon is exhausted. */
    @Modifying(flushAutomatically = true)
    @Query("update Coupon c set c.usedCount = c.usedCount + 1 where c.id = :id and (c.usageLimit is null or c.usedCount < c.usageLimit)")
    int consume(@Param("id") UUID id);
}
