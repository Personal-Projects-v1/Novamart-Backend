package commerce.novamart.order.entity;

import commerce.novamart.common.entity.BaseEntity;
import commerce.novamart.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String orderNumber;

    /** Null for guest checkouts. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 255)
    private String contactEmail;

    @Column(length = 40)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PROCESSING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal shipping = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(length = 60)
    private String couponCode;

    @Column(nullable = false, length = 60)
    private String paymentMethod;

    @Column(nullable = false, length = 60)
    private String shippingMethod;

    @Column(length = 80)
    private String trackingNumber;

    // Shipping address snapshot (orders must not change when a saved address is edited later).
    @Column(nullable = false, length = 200)
    private String shipFullName;

    @Column(nullable = false, length = 40)
    private String shipPhone;

    @Column(length = 255)
    private String shipEmail;

    @Column(nullable = false, length = 120)
    private String shipCountry;

    @Column(nullable = false, length = 120)
    private String shipProvince;

    @Column(nullable = false, length = 120)
    private String shipCity;

    @Column(length = 120)
    private String shipDistrict;

    @Column(nullable = false, length = 300)
    private String shipStreet;

    @Column(length = 30)
    private String shipPostalCode;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("occurredAt ASC")
    @BatchSize(size = 50)
    private List<OrderEvent> events = new ArrayList<>();

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void addEvent(String status, java.time.LocalDateTime at) {
        if (events.stream().anyMatch(e -> e.getStatus().equals(status))) return;
        OrderEvent event = new OrderEvent();
        event.setOrder(this);
        event.setStatus(status);
        event.setOccurredAt(at);
        events.add(event);
    }
}
