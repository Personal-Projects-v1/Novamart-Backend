package commerce.novamart.admin.service;

import commerce.novamart.admin.dto.AdminDtos.BrandRequest;
import commerce.novamart.admin.dto.AdminDtos.CategoryRequest;
import commerce.novamart.admin.dto.AdminDtos.FlashDealAdminResponse;
import commerce.novamart.admin.dto.AdminDtos.FlashDealRequest;
import commerce.novamart.admin.dto.AdminDtos.ProductRequest;
import commerce.novamart.catalog.dto.CatalogDtos.BrandResponse;
import commerce.novamart.catalog.dto.CatalogDtos.CategoryResponse;
import commerce.novamart.catalog.dto.CatalogDtos.ProductFilter;
import commerce.novamart.catalog.dto.CatalogDtos.ProductResponse;
import commerce.novamart.catalog.entity.Brand;
import commerce.novamart.catalog.entity.Category;
import commerce.novamart.catalog.entity.FlashDeal;
import commerce.novamart.catalog.entity.Product;
import commerce.novamart.catalog.repository.BrandRepository;
import commerce.novamart.catalog.repository.CategoryRepository;
import commerce.novamart.catalog.repository.FlashDealRepository;
import commerce.novamart.catalog.repository.ProductRepository;
import commerce.novamart.catalog.service.CatalogMapper;
import commerce.novamart.catalog.service.ProductSpecs;
import commerce.novamart.common.dto.PagedResult;
import commerce.novamart.common.exception.BadRequestException;
import commerce.novamart.common.exception.ResourceNotFoundException;
import commerce.novamart.common.persistence.Json;
import commerce.novamart.common.util.Dates;
import commerce.novamart.common.util.Slugs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCatalogService {

    private final ProductRepository products;
    private final BrandRepository brands;
    private final CategoryRepository categories;
    private final FlashDealRepository deals;
    private final CatalogMapper mapper;

    // ----- products -------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PagedResult<ProductResponse> listProducts(String query, String sort, int page, int pageSize) {
        var filter = new ProductFilter(null, null, null, null, null, false, false, null, null, query, 1, 1, true);
        Sort order = switch (sort == null ? "" : sort) {
            case "stock-asc" -> Sort.by("stock").ascending().and(Sort.by("name"));
            case "stock-desc" -> Sort.by("stock").descending().and(Sort.by("name"));
            case "price-asc" -> Sort.by("price").ascending();
            case "price-desc" -> Sort.by("price").descending();
            default -> Sort.by("createdAt").descending().and(Sort.by("id"));
        };
        var result = products.findAll(ProductSpecs.from(filter),
                PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(pageSize, 1), 200), order));
        var counts = mapper.counts();
        return PagedResult.of(result, p -> mapper.product(p, counts));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        return mapper.product(findProduct(id));
    }

    public ProductResponse createProduct(ProductRequest req) {
        Product p = new Product();
        p.setSlug(uniqueProductSlug(blankToNull(req.slug()) != null ? req.slug() : req.name(), null));
        p.setSku(req.sku() != null && !req.sku().isBlank() ? requireFreeSku(req.sku().trim(), null) : generateSku());
        apply(p, req);
        return mapper.product(products.save(p));
    }

    public ProductResponse updateProduct(UUID id, ProductRequest req) {
        Product p = findProduct(id);
        if (req.slug() != null && !req.slug().isBlank() && !Slugs.of(req.slug()).equals(p.getSlug())) {
            p.setSlug(uniqueProductSlug(req.slug(), p.getId()));
        }
        if (req.sku() != null && !req.sku().isBlank() && !req.sku().trim().equals(p.getSku())) {
            p.setSku(requireFreeSku(req.sku().trim(), p.getId()));
        }
        apply(p, req);
        return mapper.product(products.save(p));
    }

    public ProductResponse setStock(UUID id, int stock) {
        Product p = findProduct(id);
        p.setStock(stock);
        return mapper.product(p);
    }

    public void deleteProduct(UUID id) {
        // order history keeps its own name/price/image snapshot and the FK nulls out, so this is safe
        products.delete(findProduct(id));
    }

    private void apply(Product p, ProductRequest r) {
        Brand brand = brands.findById(r.brandId()).orElseThrow(() -> new BadRequestException("Brand not found"));
        Category category = categories.findById(r.categoryId()).orElseThrow(() -> new BadRequestException("Category not found"));
        if (r.compareAtPrice() != null && r.compareAtPrice().signum() > 0 && r.compareAtPrice().compareTo(r.price()) < 0) {
            throw new BadRequestException("Compare-at price must be higher than the price");
        }
        p.setName(r.name().trim());
        p.setBrand(brand);
        p.setCategory(category);
        p.setPrice(r.price());
        // 0 / empty means "not on sale"
        p.setCompareAtPrice(r.compareAtPrice() != null && r.compareAtPrice().signum() > 0 ? r.compareAtPrice() : null);
        p.setStock(r.stock());
        p.setShortDescription(r.shortDescription());
        p.setDescription(r.description());
        List<String> images = r.images() == null ? p.getImages() : r.images();
        p.setImages(new ArrayList<>(images));
        p.setThumbnail(blankToNull(r.thumbnail()) != null ? r.thumbnail() : images.isEmpty() ? p.getThumbnail() : images.get(0));
        if (r.badges() != null) p.setBadges(new ArrayList<>(r.badges()));
        p.setVariants(r.variants() != null ? r.variants() : p.getVariants() != null ? p.getVariants() : Json.tree("[]"));
        p.setSpecGroups(r.specGroups() != null ? r.specGroups() : p.getSpecGroups() != null ? p.getSpecGroups() : Json.tree("[]"));
        if (r.tags() != null) p.setTags(new ArrayList<>(r.tags()));
        if (r.warrantyMonths() != null) p.setWarrantyMonths(r.warrantyMonths());
        if (r.featured() != null) p.setFeatured(r.featured());
        if (r.bestSeller() != null) p.setBestSeller(r.bestSeller());
        if (r.freeShipping() != null) p.setFreeShipping(r.freeShipping());
        if (r.active() != null) p.setActive(r.active());
        p.setColors(colorLabels(p.getVariants()));
        p.setColorSwatches(colorSwatches(p.getVariants(), p.getColorSwatches()));
        p.setSearchText((p.getName() + " " + brand.getName() + " " + category.getName() + " " + String.join(" ", p.getTags()))
                .toLowerCase(Locale.ROOT));
    }

    private static Set<String> colorLabels(JsonNode variants) {
        Set<String> labels = new LinkedHashSet<>();
        if (variants != null && variants.isArray()) {
            for (JsonNode group : variants) {
                if ("Color".equals(group.path("name").asString(""))) {
                    for (JsonNode option : group.path("options")) {
                        String label = option.path("label").asString("");
                        if (!label.isBlank()) labels.add(label);
                    }
                }
            }
        }
        return labels;
    }

    private static List<String> colorSwatches(JsonNode variants, List<String> fallback) {
        List<String> swatches = new ArrayList<>();
        if (variants != null && variants.isArray()) {
            for (JsonNode group : variants) {
                if ("Color".equals(group.path("name").asString(""))) {
                    for (JsonNode option : group.path("options")) swatches.add(option.path("swatch").asString("#111827"));
                }
            }
        }
        return swatches.isEmpty() ? fallback : swatches;
    }

    // ----- brands ---------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<BrandResponse> listBrands() {
        var counts = mapper.counts();
        return brands.findAll(Sort.by("name")).stream().map(b -> mapper.brand(b, counts)).toList();
    }

    public BrandResponse createBrand(BrandRequest r) {
        Brand b = new Brand();
        b.setSlug(uniqueSlug(blankToNull(r.slug()) != null ? r.slug() : r.name(), brands::existsBySlug));
        applyBrand(b, r);
        return mapper.brand(brands.save(b), mapper.counts());
    }

    public BrandResponse updateBrand(UUID id, BrandRequest r) {
        Brand b = brands.findById(id).orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
        if (blankToNull(r.slug()) != null && !Slugs.of(r.slug()).equals(b.getSlug())) {
            b.setSlug(uniqueSlug(r.slug(), brands::existsBySlug));
        }
        applyBrand(b, r);
        return mapper.brand(b, mapper.counts());
    }

    public void deleteBrand(UUID id) {
        Brand b = brands.findById(id).orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
        if (products.countByBrandId(id) > 0) {
            throw new BadRequestException("This brand still has products. Move or delete them first.");
        }
        brands.delete(b);
    }

    private static void applyBrand(Brand b, BrandRequest r) {
        b.setName(r.name().trim());
        b.setLogo(r.logo());
        b.setDescription(r.description());
        if (r.featured() != null) b.setFeatured(r.featured());
    }

    // ----- categories -----------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        var counts = mapper.counts();
        return categories.findAll(Sort.by("createdAt", "name")).stream().map(c -> mapper.category(c, counts)).toList();
    }

    public CategoryResponse createCategory(CategoryRequest r) {
        Category c = new Category();
        c.setSlug(uniqueSlug(blankToNull(r.slug()) != null ? r.slug() : r.name(), categories::existsBySlug));
        applyCategory(c, r);
        return mapper.category(categories.save(c), mapper.counts());
    }

    public CategoryResponse updateCategory(UUID id, CategoryRequest r) {
        Category c = categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (blankToNull(r.slug()) != null && !Slugs.of(r.slug()).equals(c.getSlug())) {
            c.setSlug(uniqueSlug(r.slug(), categories::existsBySlug));
        }
        if (id.equals(r.parentId())) throw new BadRequestException("A category can't be its own parent");
        applyCategory(c, r);
        return mapper.category(c, mapper.counts());
    }

    public void deleteCategory(UUID id) {
        Category c = categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (products.countByCategoryId(id) > 0) {
            throw new BadRequestException("This category still has products. Move or delete them first.");
        }
        categories.delete(c);
    }

    private void applyCategory(Category c, CategoryRequest r) {
        c.setName(r.name().trim());
        c.setDescription(r.description());
        c.setImage(r.image());
        c.setIcon(r.icon());
        c.setParent(r.parentId() == null ? null
                : categories.findById(r.parentId()).orElseThrow(() -> new BadRequestException("Parent category not found")));
    }

    // ----- flash deals ----------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<FlashDealAdminResponse> listDeals() {
        return deals.findAllByOrderByEndsAtDesc().stream().map(AdminCatalogService::toResponse).toList();
    }

    public FlashDealAdminResponse createDeal(FlashDealRequest r) {
        Product product = findProduct(r.productId());
        if (!r.endsAt().isAfter(LocalDateTime.now())) throw new BadRequestException("The end time must be in the future");
        if (r.dealPrice().compareTo(product.getPrice()) >= 0) {
            throw new BadRequestException("The deal price must be lower than the regular price ($" + product.getPrice() + ")");
        }
        FlashDeal d = new FlashDeal();
        d.setProduct(product);
        d.setEndsAt(r.endsAt());
        d.setDealPrice(r.dealPrice());
        d.setClaimedPercent(r.claimedPercent() == null ? 0 : Math.max(0, Math.min(100, r.claimedPercent())));
        return toResponse(deals.save(d));
    }

    public void deleteDeal(UUID id) {
        deals.delete(deals.findById(id).orElseThrow(() -> new ResourceNotFoundException("Promotion not found")));
    }

    private static FlashDealAdminResponse toResponse(FlashDeal d) {
        return new FlashDealAdminResponse(d.getId().toString(), d.getProduct().getId().toString(), d.getProduct().getName(),
                Dates.iso(d.getEndsAt()), d.getClaimedPercent(), d.getDealPrice(), d.isActive(),
                d.getEndsAt().isBefore(LocalDateTime.now()));
    }

    // ----- helpers --------------------------------------------------------------------------------

    private Product findProduct(UUID id) {
        return products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    /** Slug unique among products, ignoring the product being edited (null when creating). */
    private String uniqueProductSlug(String source, UUID ignoreProductId) {
        UUID ignore = ignoreProductId == null ? new UUID(0, 0) : ignoreProductId;
        return uniqueSlug(source, slug -> products.existsBySlugAndIdNot(slug, ignore));
    }

    private String uniqueSlug(String source, Predicate<String> existing) {
        String base = Slugs.of(source);
        if (base.isBlank()) throw new BadRequestException("Couldn't derive a URL slug from that name");
        return suffixUntilFree(base, existing);
    }

    private static String suffixUntilFree(String base, Predicate<String> taken) {
        String candidate = base;
        for (int i = 2; taken.test(candidate); i++) candidate = base + "-" + i;
        return candidate;
    }

    private String requireFreeSku(String sku, UUID ignoreId) {
        if (products.existsBySkuAndIdNot(sku, ignoreId == null ? new UUID(0, 0) : ignoreId)) {
            throw new BadRequestException("SKU " + sku + " is already used by another product");
        }
        return sku;
    }

    private String generateSku() {
        String sku;
        do {
            sku = "NM-" + (100000 + new java.util.Random().nextInt(900000));
        } while (products.existsBySku(sku));
        return sku;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }
}
