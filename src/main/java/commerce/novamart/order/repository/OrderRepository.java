package commerce.novamart.order.repository;

import commerce.novamart.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Order> findByOrderNumberIgnoreCase(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    /** True if the user has a non-cancelled order containing the product (drives the "verified purchase" badge). */
    @Query("select count(o) > 0 from Order o join o.items i where o.user.id = :userId and i.product.id = :productId and o.status <> 'CANCELLED'")
    boolean hasPurchased(@Param("userId") UUID userId, @Param("productId") UUID productId);

    long countByUserId(UUID userId);

    @Query("select coalesce(sum(o.total), 0) from Order o where o.user.id = :userId and o.status <> 'CANCELLED'")
    BigDecimal lifetimeSpend(@Param("userId") UUID userId);

    @Query("select coalesce(sum(o.total), 0) from Order o where o.status <> 'CANCELLED' and o.createdAt >= :since")
    BigDecimal revenueSince(@Param("since") LocalDateTime since);

    @Query("select count(o) from Order o where o.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    @Query("select o.status, count(o) from Order o group by o.status")
    List<Object[]> countByStatus();

    @Query("select cast(o.createdAt as date), coalesce(sum(o.total), 0) from Order o where o.status <> 'CANCELLED' and o.createdAt >= :since group by cast(o.createdAt as date) order by cast(o.createdAt as date)")
    List<Object[]> revenueByDay(@Param("since") LocalDateTime since);

    @Query("select coalesce(sum(o.total), 0) from Order o where o.status <> 'CANCELLED'")
    BigDecimal totalRevenue();

    long countByStatusNot(commerce.novamart.order.entity.OrderStatus status);

    @Query("select coalesce(sum(o.total), 0) from Order o where o.status <> 'CANCELLED' and o.createdAt >= :from and o.createdAt < :to")
    BigDecimal revenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select count(o) from Order o where o.createdAt >= :from and o.createdAt < :to")
    long countBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** [userId, orderCount, spend] for the given customers (cancelled orders excluded from spend). */
    @Query("select o.user.id, count(o), coalesce(sum(case when o.status <> 'CANCELLED' then o.total else 0 end), 0) from Order o where o.user.id in :ids group by o.user.id")
    List<Object[]> statsForUsers(@Param("ids") java.util.Collection<UUID> ids);
}
