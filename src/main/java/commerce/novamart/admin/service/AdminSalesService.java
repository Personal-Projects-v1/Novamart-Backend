package commerce.novamart.admin.service;

import commerce.novamart.admin.dto.AdminDtos.CouponAdminResponse;
import commerce.novamart.admin.dto.AdminDtos.CouponRequest;
import commerce.novamart.admin.dto.AdminDtos.CustomerDetail;
import commerce.novamart.admin.dto.AdminDtos.CustomerSummary;
import commerce.novamart.admin.dto.AdminDtos.DailyRevenue;
import commerce.novamart.admin.dto.AdminDtos.Dashboard;
import commerce.novamart.admin.dto.AdminDtos.LowStockItem;
import commerce.novamart.admin.dto.AdminDtos.PopularItem;
import commerce.novamart.admin.dto.AdminDtos.Trend;
import commerce.novamart.catalog.entity.Product;
import commerce.novamart.catalog.repository.ProductRepository;
import commerce.novamart.common.dto.PagedResult;
import commerce.novamart.common.exception.BadRequestException;
import commerce.novamart.common.exception.ResourceNotFoundException;
import commerce.novamart.common.util.Dates;
import commerce.novamart.coupon.entity.Coupon;
import commerce.novamart.coupon.repository.CouponRepository;
import commerce.novamart.order.dto.OrderDtos.OrderResponse;
import commerce.novamart.order.entity.Order;
import commerce.novamart.order.entity.OrderStatus;
import commerce.novamart.order.repository.OrderRepository;
import commerce.novamart.order.service.OrderMapper;
import commerce.novamart.order.service.OrderService;
import commerce.novamart.user.entity.Role;
import commerce.novamart.user.entity.User;
import commerce.novamart.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminSalesService {

    private final OrderRepository orders;
    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final UserRepository users;
    private final CouponRepository coupons;
    private final ProductRepository products;

    // ----- dashboard ------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Dashboard dashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime d30 = now.minusDays(30);
        LocalDateTime d60 = now.minusDays(60);

        BigDecimal revenueTotal = orders.totalRevenue();
        long orderTotal = orders.count();
        long customerTotal = users.countByRole(Role.CUSTOMER);
        long paidOrders = orders.countByStatusNot(OrderStatus.CANCELLED);

        var revenue = new Trend(revenueTotal, change(orders.revenueBetween(d30, now.plusMinutes(1)), orders.revenueBetween(d60, d30)));
        var orderTrend = new Trend(BigDecimal.valueOf(orderTotal),
                change(BigDecimal.valueOf(orders.countBetween(d30, now.plusMinutes(1))), BigDecimal.valueOf(orders.countBetween(d60, d30))));
        var customerTrend = new Trend(BigDecimal.valueOf(customerTotal), change(
                BigDecimal.valueOf(users.countByRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Role.CUSTOMER, d30, now.plusMinutes(1))),
                BigDecimal.valueOf(users.countByRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Role.CUSTOMER, d60, d30))));
        BigDecimal aov = paidOrders == 0 ? BigDecimal.ZERO
                : revenueTotal.divide(BigDecimal.valueOf(paidOrders), 2, RoundingMode.HALF_UP);

        List<OrderResponse> recent = orders.findAll(PageRequest.of(0, 6, Sort.by("createdAt").descending())).stream()
                .map(orderMapper::toResponse).toList();

        List<LowStockItem> lowStock = products.findAll(Sort.by("stock")).stream()
                .filter(p -> p.isActive() && p.getStock() <= 10)
                .limit(8).map(p -> new LowStockItem(p.getId().toString(), p.getName(), p.getStock())).toList();

        List<PopularItem> popular = products.findTrending(PageRequest.of(0, 5)).stream()
                .map(p -> new PopularItem(p.getId().toString(), p.getName(), p.getReviewCount())).toList();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : orders.countByStatus()) byStatus.put(((OrderStatus) row[0]).api(), (Long) row[1]);

        List<DailyRevenue> daily = new ArrayList<>();
        for (Object[] row : orders.revenueByDay(now.minusDays(14))) {
            daily.add(new DailyRevenue(String.valueOf(row[0]), (BigDecimal) row[1]));
        }
        return new Dashboard(revenue, orderTrend, customerTrend, aov, recent, lowStock, popular, byStatus, daily);
    }

    /** Percent change of the last 30 days vs the 30 before; null when there is nothing to compare against. */
    private static Double change(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) return null;
        return current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP).doubleValue();
    }

    // ----- orders ---------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PagedResult<OrderResponse> listOrders(String status, String query, int page, int pageSize) {
        OrderStatus wanted = status == null || status.isBlank() ? null : OrderStatus.fromApi(status);
        String q = query == null || query.isBlank() ? null : "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        Specification<Order> spec = (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (wanted != null) ps.add(cb.equal(root.get("status"), wanted));
            if (q != null) {
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("orderNumber")), q),
                        cb.like(cb.lower(root.get("shipFullName")), q),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("contactEmail"), "")), q)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        var result = orders.findAll(spec, PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(pageSize, 1), 100),
                Sort.by("createdAt").descending()));
        return PagedResult.of(result, orderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        return orderMapper.toResponse(orders.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found")));
    }

    public OrderResponse updateOrderStatus(UUID id, String status, String trackingNumber) {
        return orderService.updateStatus(id, OrderStatus.fromApi(status), trackingNumber);
    }

    // ----- customers ------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PagedResult<CustomerSummary> listCustomers(String query, int page, int pageSize) {
        var pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(pageSize, 1), 100), Sort.by("createdAt").descending());
        var result = query == null || query.isBlank()
                ? users.findByRole(Role.CUSTOMER, pageable)
                : users.searchCustomers("%" + query.trim().toLowerCase(Locale.ROOT) + "%", pageable);

        Map<UUID, Object[]> stats = new HashMap<>();
        if (!result.isEmpty()) {
            for (Object[] row : orders.statsForUsers(result.getContent().stream().map(User::getId).toList())) {
                stats.put((UUID) row[0], row);
            }
        }
        return PagedResult.of(result, u -> summary(u, stats.get(u.getId())));
    }

    @Transactional(readOnly = true)
    public CustomerDetail customer(UUID id) {
        User u = findCustomer(id);
        List<Order> theirOrders = orders.findByUserIdOrderByCreatedAtDesc(id);
        var stats = orders.statsForUsers(List.of(id)).stream().findFirst().orElse(null);
        return new CustomerDetail(summary(u, stats), theirOrders.stream().map(orderMapper::toResponse).toList());
    }

    public CustomerSummary setCustomerEnabled(UUID id, boolean enabled) {
        User u = findCustomer(id);
        u.setEnabled(enabled);
        return summary(u, orders.statsForUsers(List.of(id)).stream().findFirst().orElse(null));
    }

    private User findCustomer(UUID id) {
        return users.findById(id).filter(u -> u.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private static CustomerSummary summary(User u, Object[] stats) {
        return new CustomerSummary(u.getId().toString(), u.getFirstName(), u.getLastName(), u.getEmail(), u.getPhone(),
                Dates.iso(u.getCreatedAt()), Boolean.TRUE.equals(u.getEnabled()), Boolean.TRUE.equals(u.getEmailVerified()),
                stats == null ? 0 : (Long) stats[1], stats == null ? BigDecimal.ZERO : (BigDecimal) stats[2]);
    }

    // ----- coupons --------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CouponAdminResponse> listCoupons() {
        return coupons.findAll(Sort.by("expiresAt").descending()).stream().map(AdminSalesService::toResponse).toList();
    }

    public CouponAdminResponse createCoupon(CouponRequest r) {
        String code = r.code().trim().toUpperCase(Locale.ROOT);
        if (coupons.findByCodeIgnoreCase(code).isPresent()) throw new BadRequestException("A coupon with code " + code + " already exists");
        Coupon c = new Coupon();
        c.setCode(code);
        apply(c, r);
        return toResponse(coupons.save(c));
    }

    public CouponAdminResponse updateCoupon(UUID id, CouponRequest r) {
        Coupon c = coupons.findById(id).orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        String code = r.code().trim().toUpperCase(Locale.ROOT);
        coupons.findByCodeIgnoreCase(code).filter(o -> !o.getId().equals(id)).ifPresent(o -> {
            throw new BadRequestException("A coupon with code " + code + " already exists");
        });
        c.setCode(code);
        apply(c, r);
        return toResponse(c);
    }

    public void deleteCoupon(UUID id) {
        coupons.delete(coupons.findById(id).orElseThrow(() -> new ResourceNotFoundException("Coupon not found")));
    }

    private static void apply(Coupon c, CouponRequest r) {
        Coupon.Type type;
        try {
            type = Coupon.Type.valueOf(r.type().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Coupon type must be \"percent\" or \"fixed\"");
        }
        if (type == Coupon.Type.PERCENT && r.value().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("A percentage coupon can't exceed 100%");
        }
        c.setType(type);
        c.setValue(r.value());
        c.setMinSubtotal(r.minSubtotal() != null && r.minSubtotal().signum() > 0 ? r.minSubtotal() : null);
        c.setExpiresAt(r.expiresAt());
        c.setDescription(r.description());
        c.setUsageLimit(r.usageLimit() != null && r.usageLimit() > 0 ? r.usageLimit() : null);
        if (r.active() != null) c.setActive(r.active());
    }

    private static CouponAdminResponse toResponse(Coupon c) {
        return new CouponAdminResponse(c.getId().toString(), c.getCode(), c.getType().name().toLowerCase(Locale.ROOT),
                c.getValue(), c.getMinSubtotal(), Dates.iso(c.getExpiresAt()), c.getDescription(), c.isActive(),
                c.getUsageLimit(), c.getUsedCount(), c.isExpired());
    }
}
