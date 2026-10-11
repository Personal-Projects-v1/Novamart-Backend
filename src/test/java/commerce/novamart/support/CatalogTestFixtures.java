package commerce.novamart.support;

import commerce.novamart.catalog.entity.Brand;
import commerce.novamart.catalog.entity.Category;
import commerce.novamart.catalog.entity.Product;
import commerce.novamart.catalog.repository.BrandRepository;
import commerce.novamart.catalog.repository.CategoryRepository;
import commerce.novamart.catalog.repository.ProductRepository;
import commerce.novamart.common.persistence.Json;
import commerce.novamart.coupon.entity.Coupon;
import commerce.novamart.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CatalogTestFixtures {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;

    public Brand brand(String slug, String name) {
        Brand b = new Brand();
        b.setSlug(slug);
        b.setName(name);
        b.setDescription("Test brand " + name);
        b.setFeatured(false);
        b.setActive(true);
        return brandRepository.save(b);
    }

    public Category category(String slug, String name) {
        Category c = new Category();
        c.setSlug(slug);
        c.setName(name);
        c.setDescription("Test category " + name);
        c.setImage("https://example.com/cat.png");
        c.setIcon("Box");
        c.setActive(true);
        return categoryRepository.save(c);
    }

    public Product product(Brand brand, Category category, String slug, String name, BigDecimal price, int stock) {
        return product(brand, category, slug, name, price, null, stock, name.toLowerCase(), List.of());
    }

    public Product product(Brand brand, Category category, String slug, String name,
                           BigDecimal price, BigDecimal compareAtPrice, int stock,
                           String searchText, List<String> tags) {
        Product p = new Product();
        p.setSlug(slug);
        p.setSku("TST-" + slug.toUpperCase().replace('-', '_'));
        p.setName(name);
        p.setBrand(brand);
        p.setCategory(category);
        p.setThumbnail("https://example.com/" + slug + ".png");
        p.setImages(List.of(p.getThumbnail()));
        p.setPrice(price);
        p.setCompareAtPrice(compareAtPrice);
        p.setCurrency("USD");
        p.setRating(new BigDecimal("4.50"));
        p.setReviewCount(10);
        p.setStock(stock);
        p.setBadges(List.of());
        p.setShortDescription("Short " + name);
        p.setDescription("Description " + name);
        p.setVariants(Json.tree("[]"));
        p.setSpecGroups(Json.tree("[]"));
        p.setTags(tags);
        p.setFeatured(false);
        p.setBestSeller(false);
        p.setFreeShipping(false);
        p.setActive(true);
        p.setWarrantyMonths(12);
        p.setColorSwatches(List.of());
        p.setSearchText(searchText.toLowerCase());
        p.setColors(Set.of());
        return productRepository.save(p);
    }

    public Coupon percentCoupon(String code, int percentOff, BigDecimal minSubtotal) {
        Coupon c = new Coupon();
        c.setCode(code);
        c.setType(Coupon.Type.PERCENT);
        c.setValue(BigDecimal.valueOf(percentOff));
        c.setMinSubtotal(minSubtotal);
        c.setExpiresAt(LocalDateTime.now().plusDays(30));
        c.setDescription("Test coupon");
        c.setActive(true);
        c.setUsedCount(0);
        return couponRepository.save(c);
    }

    public UUID id(Product product) {
        return product.getId();
    }
}
