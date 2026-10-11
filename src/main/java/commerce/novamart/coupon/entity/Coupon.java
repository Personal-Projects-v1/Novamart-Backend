package commerce.novamart.coupon.entity;

import commerce.novamart.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
public class Coupon extends BaseEntity {

    public enum Type { PERCENT, FIXED }

    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(precision = 12, scale = 2)
    private BigDecimal minSubtotal;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    private Integer usageLimit;

    @Column(nullable = false)
    private int usedCount;

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    /** Discount this coupon gives on the given subtotal (never more than the subtotal). */
    public BigDecimal discountFor(BigDecimal subtotal) {
        BigDecimal raw = type == Type.PERCENT
                ? subtotal.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : value;
        return raw.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }
}
