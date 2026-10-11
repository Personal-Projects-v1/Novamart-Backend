package commerce.novamart.admin.dto;

import commerce.novamart.catalog.dto.CatalogDtos.ProductResponse;
import commerce.novamart.order.dto.OrderDtos.OrderResponse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class AdminDtos {

    private AdminDtos() {
    }

    // ----- catalog --------------------------------------------------------------------------------

    public record ProductRequest(
            @NotBlank(message = "Name is required") @Size(max = 255) String name,
            @Size(max = 220) String slug,
            @Size(max = 80) String sku,
            @NotNull(message = "Brand is required") UUID brandId,
            @NotNull(message = "Category is required") UUID categoryId,
            @NotNull(message = "Price is required") @DecimalMin(value = "0.00", message = "Price can't be negative") BigDecimal price,
            @DecimalMin(value = "0.00", message = "Compare-at price can't be negative") BigDecimal compareAtPrice,
            @Min(value = 0, message = "Stock can't be negative") int stock,
            String shortDescription,
            String description,
            List<String> images,
            String thumbnail,
            List<String> badges,
            JsonNode variants,
            JsonNode specGroups,
            List<String> tags,
            Integer warrantyMonths,
            Boolean featured,
            Boolean bestSeller,
            Boolean freeShipping,
            Boolean active) {
    }

    public record StockRequest(@Min(value = 0, message = "Stock can't be negative") int stock) {
    }

    public record BrandRequest(
            @NotBlank(message = "Name is required") @Size(max = 160) String name,
            @Size(max = 160) String slug,
            @Size(max = 500) String logo,
            String description,
            Boolean featured) {
    }

    public record CategoryRequest(
            @NotBlank(message = "Name is required") @Size(max = 160) String name,
            @Size(max = 160) String slug,
            String description,
            @Size(max = 500) String image,
            @Size(max = 60) String icon,
            UUID parentId) {
    }

    // ----- sales ----------------------------------------------------------------------------------

    public record CouponRequest(
            @NotBlank(message = "Code is required") @Size(max = 60) String code,
            @NotBlank(message = "Type is required") String type,
            @NotNull(message = "Value is required") @DecimalMin(value = "0.01", message = "Value must be positive") BigDecimal value,
            @DecimalMin(value = "0.00") BigDecimal minSubtotal,
            @NotNull(message = "Expiry date is required") LocalDateTime expiresAt,
            @Size(max = 500) String description,
            Integer usageLimit,
            Boolean active) {
    }

    public record CouponAdminResponse(
            String id, String code, String type, BigDecimal value, BigDecimal minSubtotal, String expiresAt,
            String description, boolean active, Integer usageLimit, int usedCount, boolean expired) {
    }

    public record FlashDealRequest(
            @NotNull(message = "Product is required") UUID productId,
            @NotNull(message = "End time is required") LocalDateTime endsAt,
            @NotNull(message = "Deal price is required") @DecimalMin(value = "0.01", message = "Deal price must be positive") BigDecimal dealPrice,
            Integer claimedPercent) {
    }

    public record FlashDealAdminResponse(
            String id, String productId, String productName, String endsAt, int claimedPercent, BigDecimal dealPrice,
            boolean active, boolean ended) {
    }

    public record SetEnabledRequest(boolean enabled) {
    }

    public record ReviewStatusRequest(@NotBlank(message = "Status is required") String status) {
    }

    public record CustomerSummary(
            String id, String firstName, String lastName, String email, String phone, String createdAt, boolean enabled,
            boolean emailVerified, long orderCount, BigDecimal totalSpent) {
    }

    public record CustomerDetail(CustomerSummary customer, List<OrderResponse> orders) {
    }

    // ----- dashboard ------------------------------------------------------------------------------

    /** Percentage change versus the previous 30 days (null when there is no previous data to compare). */
    public record Trend(BigDecimal value, Double changePercent) {
    }

    public record LowStockItem(String id, String name, int stock) {
    }

    public record PopularItem(String id, String name, int reviewCount) {
    }

    public record DailyRevenue(String date, BigDecimal revenue) {
    }

    public record Dashboard(
            Trend revenue, Trend orders, Trend customers, BigDecimal averageOrderValue,
            List<OrderResponse> recentOrders, List<LowStockItem> lowStock, List<PopularItem> popular,
            java.util.Map<String, Long> ordersByStatus, List<DailyRevenue> revenueByDay) {
    }

    public record ProductPage(List<ProductResponse> items, long total, int page, int pageSize, int totalPages) {
    }
}
