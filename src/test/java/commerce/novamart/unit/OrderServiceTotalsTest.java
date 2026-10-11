package commerce.novamart.unit;

import commerce.novamart.order.service.OrderService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceTotalsTest {

    @Test
    void standardShippingAppliesWhenSubtotalAtOrBelowThreshold() {
        OrderService.Totals t = OrderService.totals(new BigDecimal("150.00"), BigDecimal.ZERO, "standard");

        assertThat(t.shipping()).isEqualByComparingTo("9.99");
        assertThat(t.tax()).isEqualByComparingTo("7.50");
        assertThat(t.total()).isEqualByComparingTo("167.49");
    }

    @Test
    void standardShippingIsFreeAboveThreshold() {
        OrderService.Totals t = OrderService.totals(new BigDecimal("250.00"), BigDecimal.ZERO, "standard");

        assertThat(t.shipping()).isEqualByComparingTo("0");
        assertThat(t.tax()).isEqualByComparingTo("12.50");
        assertThat(t.total()).isEqualByComparingTo("262.50");
    }

    @Test
    void expressShippingUsesFlatRate() {
        OrderService.Totals t = OrderService.totals(new BigDecimal("50.00"), BigDecimal.ZERO, "express");

        assertThat(t.shipping()).isEqualByComparingTo("19.99");
    }

    @Test
    void discountReducesTaxableAmount() {
        OrderService.Totals t = OrderService.totals(new BigDecimal("100.00"), new BigDecimal("10.00"), "pickup");

        assertThat(t.discount()).isEqualByComparingTo("10.00");
        assertThat(t.shipping()).isEqualByComparingTo("0");
        assertThat(t.tax()).isEqualByComparingTo("4.50");
        assertThat(t.total()).isEqualByComparingTo("94.50");
    }
}
