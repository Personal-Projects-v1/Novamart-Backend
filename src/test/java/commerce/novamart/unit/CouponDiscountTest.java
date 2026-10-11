package commerce.novamart.unit;

import commerce.novamart.coupon.entity.Coupon;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CouponDiscountTest {

    @Test
    void percentCouponComputesExpectedDiscount() {
        Coupon coupon = new Coupon();
        coupon.setType(Coupon.Type.PERCENT);
        coupon.setValue(new BigDecimal("10"));

        assertThat(coupon.discountFor(new BigDecimal("200.00"))).isEqualByComparingTo("20.00");
    }

    @Test
    void fixedCouponNeverExceedsSubtotal() {
        Coupon coupon = new Coupon();
        coupon.setType(Coupon.Type.FIXED);
        coupon.setValue(new BigDecimal("25"));

        assertThat(coupon.discountFor(new BigDecimal("15.00"))).isEqualByComparingTo("15.00");
    }
}
