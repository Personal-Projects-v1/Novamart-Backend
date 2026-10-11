package commerce.novamart.coupon.service;

import commerce.novamart.common.exception.BadRequestException;
import commerce.novamart.common.util.Dates;
import commerce.novamart.coupon.entity.Coupon;
import commerce.novamart.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    /** Matches the frontend {@code Coupon} type ("percent" | "fixed"). */
    public record CouponResponse(String code, String type, BigDecimal value, BigDecimal minSubtotal,
                                 String expiresAt, String description) {
    }

    private final CouponRepository repository;

    public List<CouponResponse> active() {
        return repository.findByActiveTrueAndExpiresAtAfterOrderByExpiresAtAsc(LocalDateTime.now()).stream()
                .filter(c -> c.getUsageLimit() == null || c.getUsedCount() < c.getUsageLimit())
                .map(CouponService::toResponse).toList();
    }

    /** Validates a code against a subtotal; throws {@link BadRequestException} with a user-facing message. */
    public Coupon validate(String code, BigDecimal subtotal) {
        Coupon coupon = repository.findByCodeIgnoreCase(code == null ? "" : code.trim())
                .filter(Coupon::isActive)
                .orElseThrow(() -> new BadRequestException("This coupon code doesn't exist."));
        if (coupon.isExpired()) {
            throw new BadRequestException("This coupon has expired.");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BadRequestException("This coupon has reached its usage limit.");
        }
        if (coupon.getMinSubtotal() != null && subtotal.compareTo(coupon.getMinSubtotal()) < 0) {
            throw new BadRequestException("Minimum order of $" + coupon.getMinSubtotal().stripTrailingZeros().toPlainString()
                    + " required for this coupon.");
        }
        return coupon;
    }

    public static CouponResponse toResponse(Coupon c) {
        return new CouponResponse(c.getCode(), c.getType().name().toLowerCase(Locale.ROOT), c.getValue(),
                c.getMinSubtotal(), Dates.iso(c.getExpiresAt()), c.getDescription());
    }
}
