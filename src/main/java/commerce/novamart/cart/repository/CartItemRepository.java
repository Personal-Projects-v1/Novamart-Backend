package commerce.novamart.cart.repository;

import commerce.novamart.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<CartItem> findByIdAndUserId(UUID id, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CartItem c where c.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
