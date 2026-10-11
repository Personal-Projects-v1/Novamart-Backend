package commerce.novamart.catalog.service;

import commerce.novamart.catalog.dto.CatalogDtos.BrandResponse;
import commerce.novamart.catalog.dto.CatalogDtos.CategoryResponse;
import commerce.novamart.catalog.dto.CatalogDtos.CollectionResponse;
import commerce.novamart.catalog.dto.CatalogDtos.FlashDealResponse;
import commerce.novamart.catalog.dto.CatalogDtos.ProductFilter;
import commerce.novamart.catalog.dto.CatalogDtos.ProductResponse;
import commerce.novamart.catalog.dto.CatalogDtos.SuggestionsResponse;
import commerce.novamart.catalog.entity.Product;
import commerce.novamart.catalog.repository.BrandRepository;
import commerce.novamart.catalog.repository.CategoryRepository;
import commerce.novamart.catalog.repository.FlashDealRepository;
import commerce.novamart.catalog.repository.ProductCollectionRepository;
import commerce.novamart.catalog.repository.ProductRepository;
import commerce.novamart.common.dto.PagedResult;
import commerce.novamart.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductCollectionRepository collectionRepository;
    private final FlashDealRepository flashDealRepository;
    private final CatalogMapper mapper;

    // ----- categories & brands ------------------------------------------------------------------

    public List<CategoryResponse> categories() {
        var counts = mapper.counts();
        return categoryRepository.findByActiveTrueOrderByCreatedAtAscNameAsc().stream()
                .map(c -> mapper.category(c, counts)).toList();
    }

    public CategoryResponse category(String slug) {
        var cat = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return mapper.category(cat, mapper.counts());
    }

    public List<BrandResponse> brands() {
        var counts = mapper.counts();
        return brandRepository.findByActiveTrueOrderByNameAsc().stream().map(b -> mapper.brand(b, counts)).toList();
    }

    public BrandResponse brand(String slug) {
        var brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
        return mapper.brand(brand, mapper.counts());
    }

    // ----- products -----------------------------------------------------------------------------

    public PagedResult<ProductResponse> products(ProductFilter filter) {
        Pageable pageable = PageRequest.of(Math.max(filter.page(), 1) - 1, clamp(filter.pageSize()), sortFor(filter.sort()));
        var page = productRepository.findAll(ProductSpecs.from(filter), pageable);
        var counts = mapper.counts();
        return PagedResult.of(page, p -> mapper.product(p, counts));
    }

    public ProductResponse productBySlug(String slug) {
        Product p = productRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return mapper.product(p);
    }

    /** Looks products up by id (used by the cart/wishlist/compare/recently-viewed views). */
    public List<ProductResponse> productsByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) return List.of();
        return mapper.products(productRepository.findByIdInAndActiveTrue(ids));
    }

    public List<ProductResponse> related(UUID productId, int limit) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return mapper.products(productRepository.findRelated(p.getId(), p.getCategory().getId(), p.getBrand().getId(),
                PageRequest.of(0, clamp(limit))));
    }

    public List<ProductResponse> featured(int limit) {
        return mapper.products(productRepository.findFeatured(PageRequest.of(0, clamp(limit))));
    }

    public List<ProductResponse> bestSellers(int limit) {
        return mapper.products(productRepository.findBestSellers(PageRequest.of(0, clamp(limit))));
    }

    public List<ProductResponse> newArrivals(int limit) {
        return mapper.products(productRepository.findNewest(PageRequest.of(0, clamp(limit))));
    }

    public List<ProductResponse> trending(int limit) {
        return mapper.products(productRepository.findTrending(PageRequest.of(0, clamp(limit))));
    }

    public List<ProductResponse> recommended(int limit) {
        return mapper.products(productRepository.findTopRated(PageRequest.of(0, clamp(limit))));
    }

    public SuggestionsResponse suggestions(String q, int limit) {
        if (q == null || q.isBlank()) return new SuggestionsResponse(List.of(), List.of());
        var filter = new ProductFilter(null, null, null, null, null, false, false, null,
                "recommended", q, 1, limit, false);
        var products = mapper.products(productRepository.findAll(ProductSpecs.from(filter),
                PageRequest.of(0, clamp(limit), sortFor("recommended"))).getContent());
        String needle = q.toLowerCase().trim();
        var categories = categoryRepository.findByActiveTrueOrderByCreatedAtAscNameAsc().stream()
                .map(c -> c.getName()).filter(n -> n.toLowerCase().contains(needle)).toList();
        return new SuggestionsResponse(products, categories);
    }

    // ----- collections & deals ------------------------------------------------------------------

    public List<CollectionResponse> collections() {
        return collectionRepository.findAll(Sort.by("createdAt")).stream().map(c -> mapper.collection(c, false)).toList();
    }

    public CollectionResponse collection(String slug) {
        var col = collectionRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
        return mapper.collection(col, true);
    }

    public List<FlashDealResponse> flashDeals() {
        var counts = mapper.counts();
        return flashDealRepository.findByActiveTrueAndEndsAtAfterOrderByEndsAtAsc(LocalDateTime.now()).stream()
                .filter(d -> d.getProduct().isActive())
                .map(d -> mapper.flashDeal(d, counts)).toList();
    }

    // ----- helpers ------------------------------------------------------------------------------

    private static int clamp(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    public static Sort sortFor(String sort) {
        if (sort == null) sort = "recommended";
        return switch (sort) {
            case "price-asc" -> Sort.by("price").ascending().and(Sort.by("id"));
            case "price-desc" -> Sort.by("price").descending().and(Sort.by("id"));
            case "newest" -> Sort.by("createdAt").descending().and(Sort.by("id"));
            case "rating" -> Sort.by("rating").descending().and(Sort.by("reviewCount").descending()).and(Sort.by("id"));
            case "bestselling" -> Sort.by("bestSeller").descending().and(Sort.by("reviewCount").descending()).and(Sort.by("id"));
            default -> Sort.by("featured").descending().and(Sort.by("rating").descending()).and(Sort.by("id"));
        };
    }
}
