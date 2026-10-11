package commerce.novamart.admin.controller;

import commerce.novamart.admin.dto.AdminDtos.BrandRequest;
import commerce.novamart.admin.dto.AdminDtos.CategoryRequest;
import commerce.novamart.admin.dto.AdminDtos.CouponAdminResponse;
import commerce.novamart.admin.dto.AdminDtos.CouponRequest;
import commerce.novamart.admin.dto.AdminDtos.CustomerDetail;
import commerce.novamart.admin.dto.AdminDtos.CustomerSummary;
import commerce.novamart.admin.dto.AdminDtos.Dashboard;
import commerce.novamart.admin.dto.AdminDtos.FlashDealAdminResponse;
import commerce.novamart.admin.dto.AdminDtos.FlashDealRequest;
import commerce.novamart.admin.dto.AdminDtos.ProductRequest;
import commerce.novamart.admin.dto.AdminDtos.ReviewStatusRequest;
import commerce.novamart.admin.dto.AdminDtos.SetEnabledRequest;
import commerce.novamart.admin.dto.AdminDtos.StockRequest;
import commerce.novamart.admin.service.AdminCatalogService;
import commerce.novamart.admin.service.AdminSalesService;
import commerce.novamart.catalog.dto.CatalogDtos.BrandResponse;
import commerce.novamart.catalog.dto.CatalogDtos.CategoryResponse;
import commerce.novamart.catalog.dto.CatalogDtos.ProductResponse;
import commerce.novamart.common.dto.ApiResponse;
import commerce.novamart.common.dto.PagedResult;
import commerce.novamart.common.exception.BadRequestException;
import commerce.novamart.order.dto.OrderDtos.OrderResponse;
import commerce.novamart.order.dto.OrderDtos.UpdateStatusRequest;
import commerce.novamart.review.dto.ReviewDtos.ReviewResponse;
import commerce.novamart.review.entity.Review;
import commerce.novamart.review.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Back-office API. Access is restricted to ADMIN in SecurityConfig and re-checked here as defence in depth. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Back-office management")
public class AdminController {

    private final AdminCatalogService catalog;
    private final AdminSalesService sales;
    private final ReviewService reviews;

    @GetMapping("/dashboard")
    public ApiResponse<Dashboard> dashboard() {
        return ApiResponse.success(sales.dashboard());
    }

    // ----- products & inventory -------------------------------------------------------------------

    @GetMapping("/products")
    public ApiResponse<PagedResult<ProductResponse>> products(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(catalog.listProducts(query, sort, page, pageSize));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<ProductResponse> product(@PathVariable UUID id) {
        return ApiResponse.success(catalog.getProduct(id));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Product created", catalog.createProduct(request)));
    }

    @PutMapping("/products/{id}")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success("Product updated", catalog.updateProduct(id, request));
    }

    @PatchMapping("/products/{id}/stock")
    public ApiResponse<ProductResponse> setStock(@PathVariable UUID id, @Valid @RequestBody StockRequest request) {
        return ApiResponse.success(catalog.setStock(id, request.stock()));
    }

    @DeleteMapping("/products/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable UUID id) {
        catalog.deleteProduct(id);
        return ApiResponse.success("Product deleted", null);
    }

    // ----- brands & categories --------------------------------------------------------------------

    @GetMapping("/brands")
    public ApiResponse<List<BrandResponse>> brands() {
        return ApiResponse.success(catalog.listBrands());
    }

    @PostMapping("/brands")
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(@Valid @RequestBody BrandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(catalog.createBrand(request)));
    }

    @PutMapping("/brands/{id}")
    public ApiResponse<BrandResponse> updateBrand(@PathVariable UUID id, @Valid @RequestBody BrandRequest request) {
        return ApiResponse.success(catalog.updateBrand(id, request));
    }

    @DeleteMapping("/brands/{id}")
    public ApiResponse<Void> deleteBrand(@PathVariable UUID id) {
        catalog.deleteBrand(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> categories() {
        return ApiResponse.success(catalog.listCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(catalog.createCategory(request)));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(catalog.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable UUID id) {
        catalog.deleteCategory(id);
        return ApiResponse.success(null);
    }

    // ----- orders ---------------------------------------------------------------------------------

    @GetMapping("/orders")
    public ApiResponse<PagedResult<OrderResponse>> orders(
            @RequestParam(required = false) String status,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(sales.listOrders(status, query, page, pageSize));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<OrderResponse> order(@PathVariable UUID id) {
        return ApiResponse.success(sales.getOrder(id));
    }

    @PatchMapping("/orders/{id}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) {
        return ApiResponse.success("Order status updated", sales.updateOrderStatus(id, request.status(), request.trackingNumber()));
    }

    // ----- customers ------------------------------------------------------------------------------

    @GetMapping("/customers")
    public ApiResponse<PagedResult<CustomerSummary>> customers(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(sales.listCustomers(query, page, pageSize));
    }

    @GetMapping("/customers/{id}")
    public ApiResponse<CustomerDetail> customer(@PathVariable UUID id) {
        return ApiResponse.success(sales.customer(id));
    }

    @PatchMapping("/customers/{id}/enabled")
    public ApiResponse<CustomerSummary> setEnabled(@PathVariable UUID id, @RequestBody SetEnabledRequest request) {
        return ApiResponse.success(sales.setCustomerEnabled(id, request.enabled()));
    }

    // ----- coupons & promotions -------------------------------------------------------------------

    @GetMapping("/coupons")
    public ApiResponse<List<CouponAdminResponse>> coupons() {
        return ApiResponse.success(sales.listCoupons());
    }

    @PostMapping("/coupons")
    public ResponseEntity<ApiResponse<CouponAdminResponse>> createCoupon(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Coupon created", sales.createCoupon(request)));
    }

    @PutMapping("/coupons/{id}")
    public ApiResponse<CouponAdminResponse> updateCoupon(@PathVariable UUID id, @Valid @RequestBody CouponRequest request) {
        return ApiResponse.success(sales.updateCoupon(id, request));
    }

    @DeleteMapping("/coupons/{id}")
    public ApiResponse<Void> deleteCoupon(@PathVariable UUID id) {
        sales.deleteCoupon(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/promotions")
    public ApiResponse<List<FlashDealAdminResponse>> promotions() {
        return ApiResponse.success(catalog.listDeals());
    }

    @PostMapping("/promotions")
    public ResponseEntity<ApiResponse<FlashDealAdminResponse>> createPromotion(@Valid @RequestBody FlashDealRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Promotion created", catalog.createDeal(request)));
    }

    @DeleteMapping("/promotions/{id}")
    public ApiResponse<Void> deletePromotion(@PathVariable UUID id) {
        catalog.deleteDeal(id);
        return ApiResponse.success(null);
    }

    // ----- review moderation ----------------------------------------------------------------------

    @GetMapping("/reviews")
    public ApiResponse<PagedResult<ReviewResponse>> reviews(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(this.reviews.adminList(parseStatus(status, false), page, pageSize));
    }

    @PatchMapping("/reviews/{id}/status")
    public ApiResponse<ReviewResponse> reviewStatus(@PathVariable UUID id, @Valid @RequestBody ReviewStatusRequest request) {
        return ApiResponse.success(this.reviews.setStatus(id, parseStatus(request.status(), true)));
    }

    @DeleteMapping("/reviews/{id}")
    public ApiResponse<Void> deleteReview(@PathVariable UUID id) {
        this.reviews.adminDelete(id);
        return ApiResponse.success(null);
    }

    private static Review.Status parseStatus(String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) throw new BadRequestException("Status is required");
            return null;
        }
        try {
            return Review.Status.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Status must be approved, pending or hidden");
        }
    }
}
