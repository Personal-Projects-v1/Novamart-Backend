package commerce.novamart.catalog.repository;

import commerce.novamart.catalog.entity.FlashDeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FlashDealRepository extends JpaRepository<FlashDeal, UUID> {
    List<FlashDeal> findByActiveTrueAndEndsAtAfterOrderByEndsAtAsc(LocalDateTime now);

    List<FlashDeal> findAllByOrderByEndsAtDesc();
}
