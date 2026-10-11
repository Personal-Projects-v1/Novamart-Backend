package commerce.novamart.integration;

import commerce.novamart.catalog.entity.Brand;
import commerce.novamart.catalog.entity.Category;
import commerce.novamart.catalog.entity.Product;
import commerce.novamart.catalog.repository.ProductRepository;
import commerce.novamart.order.dto.OrderDtos.PlaceOrderRequest;
import commerce.novamart.order.service.OrderService;
import commerce.novamart.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CheckoutIntegrationTest extends IntegrationTestBase {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void guestCheckoutDecrementsStockAndComputesTotals() throws Exception {
        Brand brand = catalog.brand("checkout-brand-" + System.nanoTime(), "Checkout Brand");
        Category cat = catalog.category("checkout-cat-" + System.nanoTime(), "Checkout Cat");
        Product product = catalog.product(brand, cat, "checkout-sku-" + System.nanoTime(), "Checkout Gadget",
                new BigDecimal("100.00"), 4);
        UUID productId = product.getId();

        String body = """
                {
                  "items": [{"productId":"%s","quantity":2,"selectedVariants":{}}],
                  "address": {
                    "fullName": "Guest Buyer",
                    "phone": "+250788123456",
                    "email": "guest.checkout@example.com",
                    "country": "Rwanda",
                    "province": "Kigali",
                    "city": "Kigali",
                    "street": "KG 7 Ave"
                  },
                  "contactEmail": "guest.checkout@example.com",
                  "shippingMethod": "standard",
                  "paymentMethod": "card"
                }
                """.formatted(productId);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("processing"))
                .andExpect(jsonPath("$.data.subtotal").value(200.0))
                .andExpect(jsonPath("$.data.shipping").value(0))
                .andExpect(jsonPath("$.data.tax").value(10.0))
                .andExpect(jsonPath("$.data.total").value(210.0));

        assertThat(productRepository.findById(productId).orElseThrow().getStock()).isEqualTo(2);
    }

    @Test
    void checkoutFailsWhenStockInsufficient() {
        Brand brand = catalog.brand("low-stock-brand-" + System.nanoTime(), "Low Stock");
        Category cat = catalog.category("low-stock-cat-" + System.nanoTime(), "Low Stock");
        Product product = catalog.product(brand, cat, "one-left-" + System.nanoTime(), "Last One",
                new BigDecimal("50.00"), 1);

        var address = new commerce.novamart.order.dto.OrderDtos.ShippingAddressRequest(
                "Buyer", "+250788000111", "buyer@example.com", "Rwanda", "Kigali", "Kigali", null, "Street 1", null);
        var request = new PlaceOrderRequest(
                List.of(new commerce.novamart.order.dto.OrderDtos.OrderItemRequest(product.getId(), 2, Map.of())),
                address, "buyer@example.com", null, "standard", "card", null);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> orderService.place(null, request))
                .hasMessageContaining("enough stock");
    }

    @Test
    void couponReducesOrderTotal() throws Exception {
        catalog.percentCoupon("TEST10", 10, null);
        Brand brand = catalog.brand("coupon-brand-" + System.nanoTime(), "Coupon Brand");
        Category cat = catalog.category("coupon-cat-" + System.nanoTime(), "Coupon Cat");
        Product product = catalog.product(brand, cat, "coupon-item-" + System.nanoTime(), "Coupon Item",
                new BigDecimal("100.00"), 5);

        String body = """
                {
                  "items": [{"productId":"%s","quantity":1,"selectedVariants":{}}],
                  "address": {
                    "fullName": "Coupon User",
                    "phone": "+250788222333",
                    "email": "coupon@example.com",
                    "country": "Rwanda",
                    "province": "Kigali",
                    "city": "Kigali",
                    "street": "Street 2"
                  },
                  "contactEmail": "coupon@example.com",
                  "shippingMethod": "pickup",
                  "paymentMethod": "mobile-money",
                  "couponCode": "TEST10"
                }
                """.formatted(product.getId());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.discount").value(10.0))
                .andExpect(jsonPath("$.data.total").value(94.5));
    }
}
