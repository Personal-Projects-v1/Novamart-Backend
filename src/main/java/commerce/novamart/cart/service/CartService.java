package commerce.novamart.cart.service;

import commerce.novamart.cart.dto.CartDtos.AddCartItemRequest;
import commerce.novamart.cart.dto.CartDtos.CartItemResponse;
import commerce.novamart.cart.entity.CartItem;
import commerce.novamart.cart.repository.CartItemRepository;
import commerce.novamart.catalog.entity.Product;
import commerce.novamart.catalog.repository.ProductRepository;
import commerce.novamart.common.exception.BadRequestException;
import commerce.novamart.common.exception.ResourceNotFoundException;
import commerce.novamart.common.util.Dates;
import commerce.novamart.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private static final int MAX_LINE_QUANTITY = 99;

    private final CartItemRepository cartRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CartItemResponse> get(User user) {
        return cartRepository.findByUserIdOrderByCreatedAtAsc(user.getId()).stream().map(CartService::toResponse).toList();
    }

    public List<CartItemResponse> add(User user, AddCartItemRequest req) {
        addInternal(user, req, true);
        return get(user);
    }

    /** Folds a guest (local) cart into the server cart after login; unavailable products are skipped silently. */
    public List<CartItemResponse> merge(User user, List<AddCartItemRequest> items) {
        for (AddCartItemRequest req : items) {
            try {
                addInternal(user, req, false);
            } catch (BadRequestException | ResourceNotFoundException ignored) {
                // product gone / out of stock since the guest added it
            }
        }
        return get(user);
    }

    public List<CartItemResponse> updateQuantity(User user, UUID itemId, int quantity) {
        CartItem item = cartRepository.findByIdAndUserId(itemId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        item.setQuantity(clampToStock(item.getProduct(), quantity));
        return get(user);
    }

    public List<CartItemResponse> remove(User user, UUID itemId) {
        cartRepository.findByIdAndUserId(itemId, user.getId()).ifPresent(cartRepository::delete);
        cartRepository.flush();
        return get(user);
    }

    public void clear(User user) {
        cartRepository.deleteAllByUserId(user.getId());
    }

    private void addInternal(User user, AddCartItemRequest req, boolean failIfUnavailable) {
        Product product = productRepository.findById(req.productId())
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (product.getStock() <= 0) {
            throw new BadRequestException(product.getName() + " is out of stock");
        }
        Map<String, String> variants = req.selectedVariants() == null ? Map.of() : req.selectedVariants();

        CartItem existing = cartRepository.findByUserIdOrderByCreatedAtAsc(user.getId()).stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()) && i.getSelectedVariants().equals(variants))
                .findFirst().orElse(null);

        if (existing != null) {
            existing.setQuantity(clampToStock(product, existing.getQuantity() + req.quantity()));
        } else {
            CartItem item = new CartItem();
            item.setUser(user);
            item.setProduct(product);
            item.setQuantity(clampToStock(product, req.quantity()));
            item.setSelectedVariants(new LinkedHashMap<>(variants));
            cartRepository.save(item);
        }
    }

    private static int clampToStock(Product product, int quantity) {
        return Math.max(1, Math.min(Math.min(quantity, MAX_LINE_QUANTITY), Math.max(product.getStock(), 1)));
    }

    private static CartItemResponse toResponse(CartItem i) {
        return new CartItemResponse(i.getId().toString(), i.getProduct().getId().toString(), i.getQuantity(),
                i.getSelectedVariants(), Dates.iso(i.getCreatedAt()));
    }
}
