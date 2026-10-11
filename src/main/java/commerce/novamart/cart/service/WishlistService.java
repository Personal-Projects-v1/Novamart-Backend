package commerce.novamart.cart.service;

import commerce.novamart.cart.dto.CartDtos.WishlistItemResponse;
import commerce.novamart.cart.entity.WishlistItem;
import commerce.novamart.cart.repository.WishlistItemRepository;
import commerce.novamart.catalog.entity.Product;
import commerce.novamart.catalog.repository.ProductRepository;
import commerce.novamart.common.exception.ResourceNotFoundException;
import commerce.novamart.common.util.Dates;
import commerce.novamart.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {

    private final WishlistItemRepository repository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> list(User user) {
        return repository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(i -> i.getProduct().isActive())
                .map(i -> new WishlistItemResponse(i.getProduct().getId().toString(), Dates.iso(i.getCreatedAt())))
                .toList();
    }

    public List<WishlistItemResponse> add(User user, UUID productId) {
        addInternal(user, productId);
        return list(user);
    }

    public List<WishlistItemResponse> merge(User user, List<UUID> productIds) {
        for (UUID id : productIds) {
            try {
                addInternal(user, id);
            } catch (ResourceNotFoundException ignored) {
                // product removed since it was saved locally
            }
        }
        return list(user);
    }

    public List<WishlistItemResponse> remove(User user, UUID productId) {
        repository.findByUserIdAndProductId(user.getId(), productId).ifPresent(repository::delete);
        repository.flush();
        return list(user);
    }

    private void addInternal(User user, UUID productId) {
        if (repository.existsByUserIdAndProductId(user.getId(), productId)) return;
        Product product = productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setProduct(product);
        repository.save(item);
    }
}
