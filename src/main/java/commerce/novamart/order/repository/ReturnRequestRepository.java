package commerce.novamart.order.repository;

import commerce.novamart.order.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {
    List<ReturnRequest> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
