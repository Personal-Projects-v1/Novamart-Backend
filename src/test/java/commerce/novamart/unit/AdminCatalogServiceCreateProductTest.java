package commerce.novamart.unit;

import commerce.novamart.admin.dto.AdminDtos.ProductRequest;
import commerce.novamart.admin.service.AdminCatalogService;
import commerce.novamart.catalog.entity.Brand;
import commerce.novamart.catalog.entity.Category;
import commerce.novamart.catalog.entity.Product;
import commerce.novamart.catalog.repository.BrandRepository;
import commerce.novamart.catalog.repository.CategoryRepository;
import commerce.novamart.catalog.repository.FlashDealRepository;
import commerce.novamart.catalog.repository.ProductRepository;
import commerce.novamart.catalog.service.CatalogMapper;
import commerce.novamart.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCatalogServiceCreateProductTest {

    @Mock
    private ProductRepository products;
    @Mock
    private BrandRepository brands;
    @Mock
    private CategoryRepository categories;
    @Mock
    private FlashDealRepository deals;

    private AdminCatalogService service;
    private Brand brand;
    private Category category;
    private UUID brandId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        service = new AdminCatalogService(products, brands, categories, deals, new CatalogMapper(products));
        brandId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        brand = new Brand();
        brand.setId(brandId);
        brand.setName("Nova Audio");
        category = new Category();
        category.setId(categoryId);
        category.setName("Headphones");

        when(brands.findById(brandId)).thenReturn(Optional.of(brand));
        when(categories.findById(categoryId)).thenReturn(Optional.of(category));
    }

    private void stubProductSave() {
        when(products.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });
    }

    @Test
    void createProductSavesNormalizedProductAndReturnsMappedResponse() {
        stubProductSave();
        ProductRequest request = request("  Wireless Headphones  ", null, " SKU-123 ",
                new BigDecimal("79.99"), new BigDecimal("99.99"));

        var response = service.createProduct(request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(products).save(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Wireless Headphones");
        assertThat(saved.getSlug()).isEqualTo("wireless-headphones");
        assertThat(saved.getSku()).isEqualTo("SKU-123");
        assertThat(saved.getBrand()).isSameAs(brand);
        assertThat(saved.getCategory()).isSameAs(category);
        assertThat(saved.getPrice()).isEqualByComparingTo("79.99");
        assertThat(saved.getCompareAtPrice()).isEqualByComparingTo("99.99");
        assertThat(saved.getThumbnail()).isEqualTo("https://example.com/headphones.png");
        assertThat(saved.getSearchText()).isEqualTo("wireless headphones nova audio headphones wireless audio");

        assertThat(response.id()).isEqualTo(saved.getId().toString());
        assertThat(response.slug()).isEqualTo("wireless-headphones");
        assertThat(response.sku()).isEqualTo("SKU-123");
        assertThat(response.name()).isEqualTo("Wireless Headphones");
    }

    @Test
    void createProductUsesAvailableSlugSuffixWhenRequestedSlugIsAlreadyTaken() {
        stubProductSave();
        when(products.existsBySlugAndIdNot("custom-headset", new UUID(0, 0))).thenReturn(true);
        when(products.existsBySlugAndIdNot("custom-headset-2", new UUID(0, 0))).thenReturn(false);

        var response = service.createProduct(request("Headphones", "Custom Headset", "SKU-456",
                new BigDecimal("49.99"), null));

        assertThat(response.slug()).isEqualTo("custom-headset-2");
        verify(products).existsBySlugAndIdNot("custom-headset", new UUID(0, 0));
        verify(products).existsBySlugAndIdNot("custom-headset-2", new UUID(0, 0));
    }

    @Test
    void createProductRejectsCompareAtPriceBelowRegularPriceWithoutSaving() {
        assertThatThrownBy(() -> service.createProduct(request("Headphones", null, "SKU-789",
                new BigDecimal("100.00"), new BigDecimal("99.99"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Compare-at price must be higher than the price");
    }

    private ProductRequest request(String name, String slug, String sku, BigDecimal price, BigDecimal compareAtPrice) {
        return new ProductRequest(name, slug, sku, brandId, categoryId, price, compareAtPrice, 12,
                "Short description", "Full description", List.of("https://example.com/headphones.png"), null,
                List.of("New"), null, null, List.of("wireless", "audio"), 24,
                true, false, true, true);
    }
}
