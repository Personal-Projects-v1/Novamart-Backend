package commerce.novamart.order.dto;

import commerce.novamart.user.dto.UserDtos.AddressResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record OrderItemRequest(
            @NotNull(message = "productId is required") UUID productId,
            @Min(value = 1, message = "Quantity must be at least 1") @Max(value = 99, message = "Quantity must be at most 99") int quantity,
            Map<String, String> selectedVariants) {
    }

    public record ShippingAddressRequest(
            @NotBlank(message = "Full name is required") @Size(max = 200) String fullName,
            @NotBlank(message = "Phone is required") @Size(max = 40) String phone,
            @Email(message = "Email must be valid") @Size(max = 255) String email,
            @NotBlank(message = "Country is required") @Size(max = 120) String country,
            @NotBlank(message = "Province is required") @Size(max = 120) String province,
            @NotBlank(message = "City is required") @Size(max = 120) String city,
            @Size(max = 120) String district,
            @NotBlank(message = "Street is required") @Size(max = 300) String street,
            @Size(max = 30) String postalCode) {
    }

    /**
     * Prices are deliberately absent: the server re-prices every line from the catalog so a tampered
     * client can't set its own totals.
     */
    public record PlaceOrderRequest(
            @NotEmpty(message = "Your cart is empty") List<@Valid OrderItemRequest> items,
            @NotNull(message = "Shipping address is required") @Valid ShippingAddressRequest address,
            @Email(message = "Email must be valid") @Size(max = 255) String contactEmail,
            @Size(max = 40) String contactPhone,
            @NotBlank(message = "Shipping method is required") String shippingMethod,
            @NotBlank(message = "Payment method is required") String paymentMethod,
            @Size(max = 60) String couponCode) {
    }

    public record OrderItemResponse(String id, String productId, String productSlug, String name, String image, BigDecimal price,
                                    int quantity, Map<String, String> selectedVariants) {
    }

    public record TimelineEntry(String status, String label, String timestamp, boolean done) {
    }

    /** Matches the frontend {@code Order} type, plus a few extras for admin screens. */
    public record OrderResponse(
            String id, String orderNumber, String createdAt, String status, List<OrderItemResponse> items,
            BigDecimal subtotal, BigDecimal discount, BigDecimal shipping, BigDecimal tax, BigDecimal total,
            AddressResponse shippingAddress, String paymentMethod, String shippingMethod, List<TimelineEntry> timeline,
            String trackingNumber, String couponCode, String contactEmail, String customerId) {
    }

    public record ReturnItemsRequest(
            @NotEmpty(message = "Select at least one item") List<String> itemIds,
            @NotBlank(message = "Reason is required") @Size(max = 120) String reason,
            @Size(max = 2000) String details,
            String refundMethod) {
    }

    public record ReturnResponse(String returnId, String status) {
    }

    public record UpdateStatusRequest(@NotBlank(message = "Status is required") String status,
                                      @Size(max = 80) String trackingNumber) {
    }
}
