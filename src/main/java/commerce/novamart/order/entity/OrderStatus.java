package commerce.novamart.order.entity;

import commerce.novamart.common.exception.BadRequestException;

import java.util.List;
import java.util.Locale;

/** Order lifecycle. API values are lower-case/hyphenated ("out-for-delivery") to match the frontend. */
public enum OrderStatus {
    PROCESSING, CONFIRMED, PACKED, SHIPPED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED, RETURNED;

    /** Timeline steps in fulfilment order, as stored in {@code order_events.status}. */
    public static final List<String> STEPS = List.of(
            "placed", "payment-confirmed", "processing", "packed", "shipped", "out-for-delivery", "delivered");

    public String api() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public static OrderStatus fromApi(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (RuntimeException e) {
            throw new BadRequestException("Unknown order status: " + value);
        }
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED || this == RETURNED;
    }

    /** Index into {@link #STEPS} this status has reached, or -1 for cancelled/returned. */
    public int stepIndex() {
        return switch (this) {
            case PROCESSING -> 2;
            case CONFIRMED -> 1;
            case PACKED -> 3;
            case SHIPPED -> 4;
            case OUT_FOR_DELIVERY -> 5;
            case DELIVERED -> 6;
            case CANCELLED, RETURNED -> -1;
        };
    }
}
