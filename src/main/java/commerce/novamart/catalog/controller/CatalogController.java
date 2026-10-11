package commerce.novamart.catalog.controller;

import commerce.novamart.catalog.dto.CatalogDtos.BrandResponse;
import commerce.novamart.catalog.dto.CatalogDtos.CategoryResponse;
import commerce.novamart.catalog.dto.CatalogDtos.CollectionResponse;
import commerce.novamart.catalog.dto.CatalogDtos.FlashDealResponse;
import commerce.novamart.catalog.dto.CatalogDtos.ProductFilter;
import commerce.novamart.catalog.dto.CatalogDtos.ProductResponse;
import commerce.novamart.catalog.dto.CatalogDtos.StoreResponse;
import commerce.novamart.catalog.dto.CatalogDtos.SuggestionsResponse;
import commerce.novamart.catalog.service.CatalogService;
import commerce.novamart.catalog.service.StoreService;
import commerce.novamart.common.dto.ApiResponse;
import commerce.novamart.common.dto.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Public, read-only storefront catalog endpoints. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Products, categories, brands, collections, deals and stores")
public class CatalogController {

    private final CatalogService catalog;
    private final StoreService stores;

    @GetMapping("/categories")
    @Operation(summary = "List categories")
    public ApiResponse<List<CategoryResponse>> categories() {
        return ApiResponse.success(catalog.categories());
    }

    @GetMapping("/categories/{slug}")
    public ApiResponse<CategoryResponse> category(@PathVariable String slug) {
        return ApiResponse.success(catalog.category(slug));
    }

    @GetMapping("/brands")
    @Operation(summary = "List brands")
    public ApiResponse<List<BrandResponse>> brands() {
        return ApiResponse.success(catalog.brands());
    }

    @GetMapping("/brands/{slug}")
    public ApiResponse<BrandResponse> brand(@PathVariable String slug) {
        return ApiResponse.success(catalog.brand(slug));
    }

    @GetMapping("/products")
    @Operation(summary = "Search / filter / paginate products (page is 1-based)")
    public ApiResponse<PagedResult<ProductResponse>> products(
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> brands,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "false") boolean inStockOnly,
            @RequestParam(defaultValue = "false") boolean onSaleOnly,
            @RequestParam(required = false) List<String> colors,
            @RequestParam(defaultValue = "recommended") String sort,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        var filter = new ProductFilter(categories, brands, minPrice, maxPrice, minRating,
                inStockOnly, onSaleOnly, colors, sort, query, page, pageSize, false);
        return ApiResponse.success(catalog.products(filter));
    }

    @GetMapping("/products/featured")
    public ApiResponse<List<ProductResponse>> featured(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.success(catalog.featured(limit));
    }

    @GetMapping("/products/best-sellers")
    public ApiResponse<List<ProductResponse>> bestSellers(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.success(catalog.bestSellers(limit));
    }

    @GetMapping("/products/new-arrivals")
    public ApiResponse<List<ProductResponse>> newArrivals(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.success(catalog.newArrivals(limit));
    }

    @GetMapping("/products/trending")
    public ApiResponse<List<ProductResponse>> trending(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.success(catalog.trending(limit));
    }

    @GetMapping("/products/recommended")
    public ApiResponse<List<ProductResponse>> recommended(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.success(catalog.recommended(limit));
    }

    @GetMapping("/products/batch")
    @Operation(summary = "Fetch several products by id (cart, wishlist, compare, recently viewed)")
    public ApiResponse<List<ProductResponse>> batch(@RequestParam List<UUID> ids) {
        return ApiResponse.success(catalog.productsByIds(ids));
    }

    @GetMapping("/products/{slug}")
    public ApiResponse<ProductResponse> product(@PathVariable String slug) {
        return ApiResponse.success(catalog.productBySlug(slug));
    }

    @GetMapping("/products/{id}/related")
    public ApiResponse<List<ProductResponse>> related(@PathVariable UUID id, @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.success(catalog.related(id, limit));
    }

    @GetMapping("/search/suggestions")
    public ApiResponse<SuggestionsResponse> suggestions(@RequestParam(name = "q") String query,
                                                        @RequestParam(defaultValue = "6") int limit) {
        return ApiResponse.success(catalog.suggestions(query, limit));
    }

    @GetMapping("/collections")
    public ApiResponse<List<CollectionResponse>> collections() {
        return ApiResponse.success(catalog.collections());
    }

    @GetMapping("/collections/{slug}")
    public ApiResponse<CollectionResponse> collection(@PathVariable String slug) {
        return ApiResponse.success(catalog.collection(slug));
    }

    @GetMapping("/deals/flash")
    public ApiResponse<List<FlashDealResponse>> flashDeals() {
        return ApiResponse.success(catalog.flashDeals());
    }

    @GetMapping("/stores")
    public ApiResponse<List<StoreResponse>> stores() {
        return ApiResponse.success(stores.list());
    }
}
