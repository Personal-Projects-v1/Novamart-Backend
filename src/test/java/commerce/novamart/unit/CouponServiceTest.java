package commerce.novamart.unit;

import commerce.novamart.common.exception.BadRequestException;
import commerce.novamart.coupon.entity.Coupon;
import commerce.novamart.coupon.repository.CouponRepository;
import commerce.novamart.coupon.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository repository;

    private CouponService service;

    @BeforeEach
    void setUp() {
        service = new CouponService(repository);
    }

    @Test
    void activeReturnsCouponsWithRemainingUsageAndMapsResponse() {
        Coupon available = coupon("SAVE10", 10);
        available.setDescription("Ten percent off");

        Coupon exhausted = coupon("USED", 10);
        exhausted.setUsageLimit(2);
        exhausted.setUsedCount(2);

        when(repository.findByActiveTrueAndExpiresAtAfterOrderByExpiresAtAsc(any(LocalDateTime.class)))
                .thenReturn(List.of(available, exhausted));

        List<CouponService.CouponResponse> result = service.active();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("SAVE10");
        assertThat(result.get(0).type()).isEqualTo("percent");
        assertThat(result.get(0).value()).isEqualByComparingTo("10");
        assertThat(result.get(0).description()).isEqualTo("Ten percent off");
        verify(repository).findByActiveTrueAndExpiresAtAfterOrderByExpiresAtAsc(any(LocalDateTime.class));
    }

    @Test
    void validateTrimsCodeAndReturnsMatchingActiveCoupon() {
        Coupon coupon = coupon("SAVE10", 10);
        when(repository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(coupon));

        Coupon result = service.validate("  SAVE10  ", new BigDecimal("50.00"));

        assertThat(result).isSameAs(coupon);
        verify(repository).findByCodeIgnoreCase("SAVE10");
    }

    @Test
    void validateRejectsMissingOrInactiveCoupon() {
        when(repository.findByCodeIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validate("UNKNOWN", new BigDecimal("50.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This coupon code doesn't exist.");

        Coupon inactive = coupon("INACTIVE", 10);
        inactive.setActive(false);
        when(repository.findByCodeIgnoreCase("INACTIVE")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.validate("INACTIVE", new BigDecimal("50.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This coupon code doesn't exist.");
    }

    @Test
    void validateRejectsCouponBelowMinimumSubtotal() {
        Coupon coupon = coupon("MINIMUM", 10);
        coupon.setMinSubtotal(new BigDecimal("100.00"));
        when(repository.findByCodeIgnoreCase("MINIMUM")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> service.validate("MINIMUM", new BigDecimal("99.99")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Minimum order of $100 required for this coupon.");
    }

    @Test
    void validateRejectsCouponAtUsageLimit() {
        Coupon coupon = coupon("LIMITED", 10);
        coupon.setUsageLimit(5);
        coupon.setUsedCount(5);
        when(repository.findByCodeIgnoreCase("LIMITED")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> service.validate("LIMITED", new BigDecimal("50.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This coupon has reached its usage limit.");
    }

    private static Coupon coupon(String code, int value) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setType(Coupon.Type.PERCENT);
        coupon.setValue(BigDecimal.valueOf(value));
        coupon.setExpiresAt(LocalDateTime.now().plusDays(1));
        return coupon;
    }
}
