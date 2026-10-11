package commerce.novamart.cart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CartDtos {

    private CartDtos() {
    }

    /** Matches the frontend {@code CartItem} type. */
    public record CartItemResponse(String id, String productId, int quantity, Map<String, String> selectedVariants,
                                   String addedAt) {
    }

    public record AddCartItemRequest(
            @NotNull(message = "productId is required") UUID productId,
            @Min(value = 1, message = "Quantity must be at least 1") @Max(value = 99, message = "Quantity must be at most 99") int quantity,
            Map<String, String> selectedVariants) {
    }

    public record UpdateCartItemRequest(
            @Min(value = 1, message = "Quantity must be at least 1") @Max(value = 99, message = "Quantity must be at most 99") int quantity) {
    }

    public record MergeCartRequest(@Valid @NotNull List<AddCartItemRequest> items) {
    }

    public record WishlistItemResponse(String productId, String addedAt) {
    }

    public record MergeWishlistRequest(@NotNull List<UUID> productIds) {
    }
}
