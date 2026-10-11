package commerce.novamart.catalog.service;

import commerce.novamart.catalog.dto.CatalogDtos.BrandResponse;
import commerce.novamart.catalog.dto.CatalogDtos.CategoryResponse;
import commerce.novamart.catalog.dto.CatalogDtos.CollectionResponse;
import commerce.novamart.catalog.dto.CatalogDtos.FlashDealResponse;
import commerce.novamart.catalog.dto.CatalogDtos.ProductResponse;
import commerce.novamart.catalog.entity.Brand;
import commerce.novamart.catalog.entity.Category;
import commerce.novamart.catalog.entity.FlashDeal;
import commerce.novamart.catalog.entity.Product;
import commerce.novamart.catalog.entity.ProductCollection;
import commerce.novamart.catalog.repository.ProductRepository;
import commerce.novamart.common.util.Dates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Entity -> response mapping for the catalog. Requires an open persistence context for lazy collections. */
@Component
@RequiredArgsConstructor
public class CatalogMapper {

    private final ProductRepository productRepository;

    /** Product counts per brand / category, loaded once per mapped batch. */
    public record Counts(Map<UUID, Long> brands, Map<UUID, Long> categories) {
        long brand(UUID id) {
            return brands.getOrDefault(id, 0L);
        }

        long category(UUID id) {
            return categories.getOrDefault(id, 0L);
        }
    }

    public Counts counts() {
        return new Counts(toMap(productRepository.countByBrand()), toMap(productRepository.countByCategory()));
    }

    private static Map<UUID, Long> toMap(List<Object[]> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : rows) map.put((UUID) row[0], (Long) row[1]);
        return map;
    }

    public BrandResponse brand(Brand b, Counts c) {
        return new BrandResponse(b.getId().toString(), b.getSlug(), b.getName(), b.getLogo(), b.getDescription(),
                c.brand(b.getId()), b.isFeatured());
    }

    public CategoryResponse category(Category cat, Counts c) {
        return new CategoryResponse(cat.getId().toString(), cat.getSlug(), cat.getName(), cat.getDescription(),
                cat.getImage(), cat.getIcon(), cat.getParent() == null ? null : cat.getParent().getId().toString(),
                c.category(cat.getId()));
    }

    public ProductResponse product(Product p, Counts c) {
        return new ProductResponse(
                p.getId().toString(), p.getSlug(), p.getSku(), p.getName(),
                p.getBrand().getId().toString(), p.getCategory().getId().toString(),
                p.getImages(), p.getThumbnail(),
                p.getPrice(), p.getCompareAtPrice(), p.getCurrency(),
                p.getRating(), p.getReviewCount(), p.getStock(),
                p.getBadges(), p.getShortDescription(), p.getDescription(),
                p.getVariants(), p.getSpecGroups(), p.getTags(),
                Dates.iso(p.getCreatedAt()), p.isFeatured(), p.isBestSeller(), p.isFreeShipping(),
                p.getWarrantyMonths(), p.getColorSwatches(), p.isActive(),
                brand(p.getBrand(), c), category(p.getCategory(), c));
    }

    public List<ProductResponse> products(Collection<Product> products) {
        if (products.isEmpty()) return List.of();
        Counts counts = counts();
        return products.stream().map(p -> product(p, counts)).toList();
    }

    public ProductResponse product(Product p) {
        return product(p, counts());
    }

    public CollectionResponse collection(ProductCollection col, boolean withProducts) {
        List<String> ids = col.getProducts().stream().map(p -> p.getId().toString()).toList();
        return new CollectionResponse(col.getId().toString(), col.getSlug(), col.getTitle(), col.getDescription(),
                col.getImage(), ids, withProducts ? products(col.getProducts().stream().filter(Product::isActive).toList()) : null);
    }

    public FlashDealResponse flashDeal(FlashDeal d, Counts c) {
        return new FlashDealResponse(d.getId().toString(), d.getProduct().getId().toString(), Dates.iso(d.getEndsAt()),
                d.getClaimedPercent(), d.getDealPrice(), product(d.getProduct(), c));
    }
}
