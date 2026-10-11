package commerce.novamart.integration;

import commerce.novamart.catalog.dto.CatalogDtos.ProductFilter;
import commerce.novamart.catalog.entity.Brand;
import commerce.novamart.catalog.entity.Category;
import commerce.novamart.catalog.entity.Product;
import commerce.novamart.catalog.service.CatalogService;
import commerce.novamart.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogAndSearchIntegrationTest extends IntegrationTestBase {

    @Autowired
    private CatalogService catalogService;

    private Product phone;
    private Product earbuds;

    @BeforeEach
    void seedCatalog() {
        Brand apple = catalog.brand("apple-test-" + nano(), "Apple Test");
        Category phones = catalog.category("phones-test-" + nano(), "Phones Test");
        Category audio = catalog.category("audio-test-" + nano(), "Audio Test");

        phone = catalog.product(apple, phones, "aurora-test-" + nano(), "Aurora Phone",
                new BigDecimal("999.00"), 5);
        earbuds = catalog.product(apple, audio, "soundwave-test-" + nano(), "SoundWave Earbuds",
                new BigDecimal("179.00"), new BigDecimal("199.00"), 12,
                "soundwave pro wireless earbuds audio", java.util.List.of("audio"));
    }

    @Test
    void productBySlugIsAvailableViaApi() throws Exception {
        mockMvc.perform(get("/api/v1/products/{slug}", phone.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value(phone.getSlug()))
                .andExpect(jsonPath("$.data.name").value("Aurora Phone"));
    }

    @Test
    void searchAndPriceFilterReturnMatchingProducts() {
        var filter = new ProductFilter(
                null, null,
                new BigDecimal("100"), new BigDecimal("200"),
                null, false, false, null,
                "recommended", "earbuds", 1, 20, false);

        var page = catalogService.products(filter);

        assertThat(page.items()).extracting(p -> p.name()).contains("SoundWave Earbuds");
        assertThat(page.items()).noneMatch(p -> p.name().equals("Aurora Phone"));
    }

    @Test
    void onSaleFilterViaHttp() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("onSaleOnly", "true")
                        .param("q", "soundwave"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items[0].name").value("SoundWave Earbuds"));
    }

    @Test
    void suggestionsIncludeProductsAndCategories() {
        var suggestions = catalogService.suggestions("audio", 5);
        assertThat(suggestions.products()).isNotEmpty();
        assertThat(suggestions.categories()).isNotEmpty();
    }

    private static String nano() {
        return Long.toString(System.nanoTime());
    }
}
