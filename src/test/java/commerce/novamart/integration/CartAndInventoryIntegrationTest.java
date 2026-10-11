package commerce.novamart.integration;

import commerce.novamart.cart.dto.CartDtos.AddCartItemRequest;
import commerce.novamart.cart.service.CartService;
import commerce.novamart.catalog.entity.Brand;
import commerce.novamart.catalog.entity.Category;
import commerce.novamart.catalog.entity.Product;
import commerce.novamart.catalog.repository.ProductRepository;
import commerce.novamart.common.exception.BadRequestException;
import commerce.novamart.support.IntegrationTestBase;
import commerce.novamart.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartAndInventoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void addToCartRespectsStockCap() {
        User user = auth.customer("cart+" + System.nanoTime() + "@example.com");
        Brand brand = catalog.brand("cart-brand-" + System.nanoTime(), "Cart Brand");
        Category cat = catalog.category("cart-cat-" + System.nanoTime(), "Cart Cat");
        Product product = catalog.product(brand, cat, "limited-" + System.nanoTime(), "Limited Widget",
                new BigDecimal("29.99"), 3);

        cartService.add(user, new AddCartItemRequest(product.getId(), 2, Map.of()));
        UUID itemId = UUID.fromString(cartService.get(user).get(0).id());
        cartService.updateQuantity(user, itemId, 99);

        var line = cartService.get(user).get(0);
        assertThat(line.quantity()).isEqualTo(3);
    }

    @Test
    void decrementStockIsAtomic() {
        Brand brand = catalog.brand("inv-brand-" + System.nanoTime(), "Inv Brand");
        Category cat = catalog.category("inv-cat-" + System.nanoTime(), "Inv Cat");
        Product product = catalog.product(brand, cat, "stock-item-" + System.nanoTime(), "Stock Item",
                new BigDecimal("10.00"), 2);

        assertThat(productRepository.decrementStock(product.getId(), 1)).isEqualTo(1);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(1);
        assertThat(productRepository.decrementStock(product.getId(), 5)).isEqualTo(0);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(1);
    }

    @Test
    void cannotAddOutOfStockProduct() {
        User user = auth.customer("nostock+" + System.nanoTime() + "@example.com");
        Brand brand = catalog.brand("empty-brand-" + System.nanoTime(), "Empty Brand");
        Category cat = catalog.category("empty-cat-" + System.nanoTime(), "Empty Cat");
        Product product = catalog.product(brand, cat, "oos-" + System.nanoTime(), "Sold Out",
                new BigDecimal("5.00"), 0);

        assertThatThrownBy(() -> cartService.add(user, new AddCartItemRequest(product.getId(), 1, Map.of())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("out of stock");
    }
}
