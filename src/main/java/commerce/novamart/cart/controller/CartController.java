package commerce.novamart.cart.controller;

import commerce.novamart.cart.dto.CartDtos.AddCartItemRequest;
import commerce.novamart.cart.dto.CartDtos.CartItemResponse;
import commerce.novamart.cart.dto.CartDtos.MergeCartRequest;
import commerce.novamart.cart.dto.CartDtos.MergeWishlistRequest;
import commerce.novamart.cart.dto.CartDtos.UpdateCartItemRequest;
import commerce.novamart.cart.dto.CartDtos.WishlistItemResponse;
import commerce.novamart.cart.service.CartService;
import commerce.novamart.cart.service.WishlistService;
import commerce.novamart.common.dto.ApiResponse;
import commerce.novamart.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Cart & Wishlist", description = "Server-side cart and wishlist for signed-in customers")
public class CartController {

    private final CartService cart;
    private final WishlistService wishlist;
    private final CurrentUser currentUser;

    @GetMapping("/cart")
    public ApiResponse<List<CartItemResponse>> cart() {
        return ApiResponse.success(cart.get(currentUser.require()));
    }

    @PostMapping("/cart/items")
    public ApiResponse<List<CartItemResponse>> add(@Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.success(cart.add(currentUser.require(), request));
    }

    @PostMapping("/cart/merge")
    public ApiResponse<List<CartItemResponse>> merge(@Valid @RequestBody MergeCartRequest request) {
        return ApiResponse.success(cart.merge(currentUser.require(), request.items()));
    }

    @PatchMapping("/cart/items/{id}")
    public ApiResponse<List<CartItemResponse>> update(@PathVariable UUID id, @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success(cart.updateQuantity(currentUser.require(), id, request.quantity()));
    }

    @DeleteMapping("/cart/items/{id}")
    public ApiResponse<List<CartItemResponse>> remove(@PathVariable UUID id) {
        return ApiResponse.success(cart.remove(currentUser.require(), id));
    }

    @DeleteMapping("/cart")
    public ApiResponse<Void> clear() {
        cart.clear(currentUser.require());
        return ApiResponse.success(null);
    }

    @GetMapping("/wishlist")
    public ApiResponse<List<WishlistItemResponse>> wishlist() {
        return ApiResponse.success(wishlist.list(currentUser.require()));
    }

    @PostMapping("/wishlist/merge")
    public ApiResponse<List<WishlistItemResponse>> mergeWishlist(@Valid @RequestBody MergeWishlistRequest request) {
        return ApiResponse.success(wishlist.merge(currentUser.require(), request.productIds()));
    }

    @PostMapping("/wishlist/{productId}")
    public ApiResponse<List<WishlistItemResponse>> addToWishlist(@PathVariable UUID productId) {
        return ApiResponse.success(wishlist.add(currentUser.require(), productId));
    }

    @DeleteMapping("/wishlist/{productId}")
    public ApiResponse<List<WishlistItemResponse>> removeFromWishlist(@PathVariable UUID productId) {
        return ApiResponse.success(wishlist.remove(currentUser.require(), productId));
    }
}
