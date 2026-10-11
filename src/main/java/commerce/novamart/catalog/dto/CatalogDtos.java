package commerce.novamart.catalog.dto;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

/** Response shapes for the public catalog API. Field names mirror the frontend's TypeScript types. */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record BrandResponse(
            String id, String slug, String name, String logo, String description,
            long productCount, boolean featured) {
    }

    public record CategoryResponse(
            String id, String slug, String name, String description, String image, String icon,
            String parentId, long productCount) {
    }

    public record ProductResponse(
            String id, String slug, String sku, String name,
            String brandId, String categoryId,
            List<String> images, String thumbnail,
            BigDecimal price, BigDecimal compareAtPrice, String currency,
            BigDecimal rating, int reviewCount, int stock,
            List<String> badges, String shortDescription, String description,
            JsonNode variants, JsonNode specGroups, List<String> tags,
            String createdAt, boolean featured, boolean bestSeller, boolean freeShipping,
            int warrantyMonths, List<String> colorSwatches, boolean active,
            BrandResponse brand, CategoryResponse category) {
    }

    public record CollectionResponse(
            String id, String slug, String title, String description, String image,
            List<String> productIds, List<ProductResponse> products) {
    }

    public record FlashDealResponse(
            String id, String productId, String endsAt, int claimedPercent, BigDecimal dealPrice,
            ProductResponse product) {
    }

    public record SuggestionsResponse(List<ProductResponse> products, List<String> categories) {
    }

    public record StoreResponse(
            String id, String name, String city, String country, String address, String hours,
            List<String> services, double lat, double lng) {
    }

    /** Query parameters for {@code GET /products}. */
    public record ProductFilter(
            List<String> categorySlugs, List<String> brandSlugs,
            BigDecimal minPrice, BigDecimal maxPrice, Double minRating,
            boolean inStockOnly, boolean onSaleOnly, List<String> colors,
            String sort, String query, int page, int pageSize, boolean includeInactive) {
    }
}
