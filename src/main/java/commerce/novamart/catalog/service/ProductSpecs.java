package commerce.novamart.catalog.service;

import commerce.novamart.catalog.dto.CatalogDtos.ProductFilter;
import commerce.novamart.catalog.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ProductSpecs {

    private ProductSpecs() {
    }

    public static Specification<Product> from(ProductFilter f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (!f.includeInactive()) ps.add(cb.isTrue(root.get("active")));

            if (f.query() != null && !f.query().isBlank()) {
                for (String term : f.query().toLowerCase().trim().split("\\s+")) {
                    ps.add(cb.like(root.get("searchText"), "%" + escape(term) + "%", '\\'));
                }
            }
            if (notEmpty(f.categorySlugs())) ps.add(root.get("category").get("slug").in(f.categorySlugs()));
            if (notEmpty(f.brandSlugs())) ps.add(root.get("brand").get("slug").in(f.brandSlugs()));
            if (f.minPrice() != null) ps.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), f.minPrice()));
            if (f.maxPrice() != null) ps.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), f.maxPrice()));
            if (f.minRating() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("rating"), BigDecimal.valueOf(f.minRating())));
            }
            if (f.inStockOnly()) ps.add(cb.greaterThan(root.<Integer>get("stock"), 0));
            if (f.onSaleOnly()) {
                ps.add(cb.isNotNull(root.get("compareAtPrice")));
                ps.add(cb.greaterThan(root.<BigDecimal>get("compareAtPrice"), root.<BigDecimal>get("price")));
            }
            if (notEmpty(f.colors())) {
                // Distinct join so a product matching two selected colours is only returned once.
                query.distinct(true);
                ps.add(root.join("colors").in(f.colors()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }

    private static String escape(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
