package commerce.novamart.integration;

import commerce.novamart.admin.service.AdminCatalogService;
import commerce.novamart.auth.service.JwtService;
import commerce.novamart.catalog.entity.Brand;
import commerce.novamart.catalog.entity.Category;
import commerce.novamart.catalog.entity.Product;
import commerce.novamart.support.IntegrationTestBase;
import commerce.novamart.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminInventoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AdminCatalogService adminCatalogService;

    @Autowired
    private JwtService jwtService;

    @Test
    void adminCanUpdateProductStockViaApi() throws Exception {
        User admin = auth.admin("admin+" + System.nanoTime() + "@example.com");
        String token = jwtService.generateAccessToken(admin);

        Brand brand = catalog.brand("admin-brand-" + System.nanoTime(), "Admin Brand");
        Category cat = catalog.category("admin-cat-" + System.nanoTime(), "Admin Cat");
        Product product = catalog.product(brand, cat, "admin-stock-" + System.nanoTime(), "Admin Stock Item",
                new BigDecimal("40.00"), 1);

        mockMvc.perform(patch("/api/v1/admin/products/{id}/stock", product.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\":25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(25));

        var updated = adminCatalogService.getProduct(product.getId());
        assertThat(updated.stock()).isEqualTo(25);
    }
}
