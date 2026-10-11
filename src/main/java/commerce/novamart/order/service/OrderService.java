package commerce.novamart.order.service;

import commerce.novamart.cart.repository.CartItemRepository;
import commerce.novamart.catalog.entity.Product;
import commerce.novamart.catalog.repository.ProductRepository;
import commerce.novamart.common.exception.BadRequestException;
import commerce.novamart.common.exception.ForbiddenException;
import commerce.novamart.common.exception.ResourceNotFoundException;
import commerce.novamart.coupon.entity.Coupon;
import commerce.novamart.coupon.repository.CouponRepository;
import commerce.novamart.coupon.service.CouponService;
import commerce.novamart.notification.service.NotificationService;
import commerce.novamart.order.dto.OrderDtos.OrderItemRequest;
import commerce.novamart.order.dto.OrderDtos.OrderResponse;
import commerce.novamart.order.dto.OrderDtos.PlaceOrderRequest;
import commerce.novamart.order.dto.OrderDtos.ReturnItemsRequest;
import commerce.novamart.order.dto.OrderDtos.ReturnResponse;
import commerce.novamart.order.dto.OrderDtos.ShippingAddressRequest;
import commerce.novamart.order.entity.Order;
import commerce.novamart.order.entity.OrderItem;
import commerce.novamart.order.entity.OrderStatus;
import commerce.novamart.order.entity.ReturnRequest;
import commerce.novamart.order.repository.OrderRepository;
import commerce.novamart.order.repository.ReturnRequestRepository;
import commerce.novamart.user.entity.Role;
import commerce.novamart.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    static final BigDecimal TAX_RATE = new BigDecimal("0.05");
    static final BigDecimal EXPRESS_SHIPPING = new BigDecimal("19.99");
    static final BigDecimal STANDARD_SHIPPING = new BigDecimal("9.99");
    static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("200");

    private static final Map<String, String> PAYMENT_LABELS = Map.of(
            "card", "Credit / Debit Card",
            "mobile-money", "Mobile Money",
            "paypal", "PayPal",
            "bank", "Bank Transfer");
    private static final Map<String, String> SHIPPING_LABELS = Map.of(
            "standard", "Standard (5-7 days)",
            "express", "Express (2-3 days)",
            "pickup", "Store Pickup");

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final ReturnRequestRepository returnRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final CouponService couponService;
    private final CartItemRepository cartRepository;
    private final NotificationService notifications;
    private final OrderMapper mapper;

    public record Totals(BigDecimal subtotal, BigDecimal discount, BigDecimal shipping, BigDecimal tax, BigDecimal total) {
    }

    /** Same arithmetic the checkout page previews, so the confirmed total always matches what the shopper saw. */
    public static Totals totals(BigDecimal subtotal, BigDecimal discount, String shippingMethod) {
        BigDecimal shipping = switch (shippingMethod) {
            case "pickup" -> BigDecimal.ZERO;
            case "express" -> EXPRESS_SHIPPING;
            default -> subtotal.compareTo(FREE_SHIPPING_THRESHOLD) > 0 ? BigDecimal.ZERO : STANDARD_SHIPPING;
        };
        BigDecimal taxable = subtotal.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal tax = taxable.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = taxable.add(shipping).add(tax).setScale(2, RoundingMode.HALF_UP);
        return new Totals(subtotal, discount, shipping, tax, total);
    }

    // ----- placing ----------------------------------------------------------------------------------

    /** @param user the signed-in customer, or {@code null} for a guest checkout */
    public OrderResponse place(User user, PlaceOrderRequest req) {
        String shippingMethod = req.shippingMethod().toLowerCase(Locale.ROOT);
        String paymentMethod = req.paymentMethod().toLowerCase(Locale.ROOT);
        if (!SHIPPING_LABELS.containsKey(shippingMethod)) throw new BadRequestException("Unknown shipping method");
        if (!PAYMENT_LABELS.containsKey(paymentMethod)) throw new BadRequestException("Unknown payment method");

        String contactEmail = firstNonBlank(req.contactEmail(), req.address().email(), user == null ? null : user.getEmail());
        if (user == null && contactEmail == null) {
            throw new BadRequestException("An email address is required so we can send your order updates");
        }

        Order order = new Order();
        order.setUser(user);
        order.setContactEmail(contactEmail == null ? null : contactEmail.toLowerCase(Locale.ROOT));
        order.setContactPhone(firstNonBlank(req.contactPhone(), req.address().phone()));
        order.setPaymentMethod(PAYMENT_LABELS.get(paymentMethod));
        order.setShippingMethod(SHIPPING_LABELS.get(shippingMethod));
        applyAddress(order, req.address());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemRequest line : mergeLines(req.items())) {
            Product product = productRepository.findById(line.productId())
                    .filter(Product::isActive)
                    .orElseThrow(() -> new BadRequestException("A product in your cart is no longer available"));
            // Atomic decrement: two shoppers can't both buy the last unit.
            if (productRepository.decrementStock(product.getId(), line.quantity()) == 0) {
                throw new BadRequestException(product.getName() + " doesn't have enough stock for the quantity requested");
            }
            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setName(product.getName());
            item.setImage(product.getThumbnail());
            item.setPrice(product.getPrice());
            item.setQuantity(line.quantity());
            item.setSelectedVariants(line.selectedVariants() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(line.selectedVariants()));
            order.addItem(item);
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (req.couponCode() != null && !req.couponCode().isBlank()) {
            Coupon coupon = couponService.validate(req.couponCode(), subtotal);
            if (couponRepository.consume(coupon.getId()) == 0) {
                throw new BadRequestException("This coupon has reached its usage limit.");
            }
            discount = coupon.discountFor(subtotal);
            order.setCouponCode(coupon.getCode());
        }

        Totals t = totals(subtotal, discount, shippingMethod);
        order.setSubtotal(t.subtotal());
        order.setDiscount(t.discount());
        order.setShipping(t.shipping());
        order.setTax(t.tax());
        order.setTotal(t.total());
        order.setOrderNumber(nextOrderNumber());
        order.setStatus(OrderStatus.PROCESSING);

        LocalDateTime now = LocalDateTime.now();
        order.addEvent("placed", now);
        order.addEvent("payment-confirmed", now); // payment is simulated, so it is confirmed immediately
        order.addEvent("processing", now);

        order = orderRepository.save(order);

        if (user != null) {
            cartRepository.deleteAllByUserId(user.getId());
            notifications.notify(user, "order", "Order " + order.getOrderNumber() + " placed",
                    "Thanks for your order! We're getting it ready.", "/account/orders/" + order.getId());
        }
        log.info("Order {} placed ({} items, total {})", order.getOrderNumber(), order.getItems().size(), order.getTotal());
        return mapper.toResponse(order);
    }

    // ----- reading ----------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<OrderResponse> mine(User user) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(User user, String idOrNumber) {
        return mapper.toResponse(findAccessible(user, idOrNumber));
    }

    /** Guest-friendly lookup: the order number plus the email or phone used at checkout. */
    @Transactional(readOnly = true)
    public OrderResponse track(String orderNumber, String contact) {
        Order order = orderRepository.findByOrderNumberIgnoreCase(orderNumber.trim())
                .filter(o -> contactMatches(o, contact))
                // Same message for "no such order" and "wrong contact" so order numbers can't be probed.
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find an order matching those details"));
        return mapper.toResponse(order);
    }

    // ----- customer actions -------------------------------------------------------------------------

    public OrderResponse cancel(User user, String idOrNumber) {
        Order order = findAccessible(user, idOrNumber);
        if (!Set.of(OrderStatus.PROCESSING, OrderStatus.CONFIRMED, OrderStatus.PACKED).contains(order.getStatus())) {
            throw new BadRequestException("This order can no longer be cancelled");
        }
        applyStatus(order, OrderStatus.CANCELLED, null);
        return mapper.toResponse(order);
    }

    public ReturnResponse requestReturn(User user, String idOrNumber, ReturnItemsRequest req) {
        Order order = findAccessible(user, idOrNumber);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("Only delivered orders can be returned");
        }
        Set<String> validIds = new java.util.HashSet<>();
        order.getItems().forEach(i -> validIds.add(i.getId().toString()));
        if (!validIds.containsAll(req.itemIds())) {
            throw new BadRequestException("One or more selected items don't belong to this order");
        }
        ReturnRequest rr = new ReturnRequest();
        rr.setReference("RET-" + (10000 + RANDOM.nextInt(90000)));
        rr.setOrder(order);
        rr.setUser(user);
        rr.setReason(req.reason());
        rr.setDetails(req.details());
        rr.setItemIds(List.copyOf(req.itemIds()));
        rr.setRefundMethod("store-credit".equals(req.refundMethod()) ? "store-credit" : "original");
        returnRepository.save(rr);
        notifications.notify(user, "order", "Return " + rr.getReference() + " received",
                "We'll review your return request for order " + order.getOrderNumber() + " shortly.",
                "/account/orders/" + order.getId());
        return new ReturnResponse(rr.getReference(), rr.getStatus());
    }

    // ----- admin ------------------------------------------------------------------------------------

    public OrderResponse updateStatus(UUID orderId, OrderStatus target, String trackingNumber) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        applyStatus(order, target, trackingNumber);
        return mapper.toResponse(order);
    }

    /** Moves an order along its lifecycle, filling in intermediate timeline steps and restocking on cancel. */
    void applyStatus(Order order, OrderStatus target, String trackingNumber) {
        OrderStatus current = order.getStatus();
        if (current == target) {
            if (trackingNumber != null && !trackingNumber.isBlank()) order.setTrackingNumber(trackingNumber.trim());
            return;
        }
        if (current == OrderStatus.CANCELLED || current == OrderStatus.RETURNED) {
            throw new BadRequestException("A " + current.api() + " order can't be changed");
        }
        if (current == OrderStatus.DELIVERED && target != OrderStatus.RETURNED) {
            throw new BadRequestException("A delivered order can only be marked as returned");
        }
        if (target.stepIndex() >= 0 && target.stepIndex() < current.stepIndex()) {
            throw new BadRequestException("An order can't move back from " + current.api() + " to " + target.api());
        }

        LocalDateTime now = LocalDateTime.now();
        if (target.stepIndex() >= 0) {
            for (int i = 0; i <= target.stepIndex(); i++) order.addEvent(OrderStatus.STEPS.get(i), now);
        } else {
            order.addEvent(target.api(), now);
        }
        if (target == OrderStatus.CANCELLED || target == OrderStatus.RETURNED) {
            order.getItems().forEach(i -> {
                if (i.getProduct() != null) productRepository.incrementStock(i.getProduct().getId(), i.getQuantity());
            });
        }
        if (target == OrderStatus.SHIPPED && (trackingNumber == null || trackingNumber.isBlank()) && order.getTrackingNumber() == null) {
            order.setTrackingNumber("NM" + (100000000 + RANDOM.nextInt(900000000)));
        }
        if (trackingNumber != null && !trackingNumber.isBlank()) order.setTrackingNumber(trackingNumber.trim());
        order.setStatus(target);

        if (order.getUser() != null) {
            notifications.notify(order.getUser(), "order", "Order " + order.getOrderNumber() + ": " + target.api().replace('-', ' '),
                    "Your order status is now \"" + target.api().replace('-', ' ') + "\".", "/account/orders/" + order.getId());
        }
    }

    // ----- helpers ----------------------------------------------------------------------------------

    private Order findAccessible(User user, String idOrNumber) {
        Order order = lookup(idOrNumber);
        boolean owner = order.getUser() != null && order.getUser().getId().equals(user.getId());
        if (!owner && user.getRole() != Role.ADMIN) {
            // 404 rather than 403 so other customers' order ids can't be probed
            throw new ResourceNotFoundException("Order not found");
        }
        return order;
    }

    private Order lookup(String idOrNumber) {
        try {
            return orderRepository.findById(UUID.fromString(idOrNumber))
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        } catch (IllegalArgumentException notUuid) {
            return orderRepository.findByOrderNumberIgnoreCase(idOrNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        }
    }

    private static boolean contactMatches(Order o, String contact) {
        if (contact == null || contact.isBlank()) return false;
        String c = contact.trim();
        if (c.contains("@")) {
            return c.equalsIgnoreCase(o.getContactEmail()) || c.equalsIgnoreCase(o.getShipEmail());
        }
        String digits = c.replaceAll("\\D", "");
        return digits.length() >= 7 && (digits.equals(o.getShipPhone().replaceAll("\\D", ""))
                || (o.getContactPhone() != null && digits.equals(o.getContactPhone().replaceAll("\\D", ""))));
    }

    /** Collapses duplicate product/variant lines so stock is checked and decremented once per line. */
    private static List<OrderItemRequest> mergeLines(List<OrderItemRequest> items) {
        Map<String, OrderItemRequest> merged = new LinkedHashMap<>();
        for (OrderItemRequest i : items) {
            Map<String, String> variants = i.selectedVariants() == null ? Map.of() : i.selectedVariants();
            String key = i.productId() + "|" + new java.util.TreeMap<>(variants);
            merged.merge(key, new OrderItemRequest(i.productId(), i.quantity(), variants),
                    (a, b) -> new OrderItemRequest(a.productId(), Math.min(99, a.quantity() + b.quantity()), a.selectedVariants()));
        }
        return List.copyOf(merged.values());
    }

    private static void applyAddress(Order order, ShippingAddressRequest a) {
        order.setShipFullName(a.fullName().trim());
        order.setShipPhone(a.phone().trim());
        order.setShipEmail(blankToNull(a.email()));
        order.setShipCountry(a.country().trim());
        order.setShipProvince(a.province().trim());
        order.setShipCity(a.city().trim());
        order.setShipDistrict(blankToNull(a.district()));
        order.setShipStreet(a.street().trim());
        order.setShipPostalCode(blankToNull(a.postalCode()));
    }

    String nextOrderNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = "VX-" + (100000 + RANDOM.nextInt(900000));
            if (!orderRepository.existsByOrderNumber(candidate)) return candidate;
        }
        return "VX-" + System.currentTimeMillis();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v.trim();
        return null;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
