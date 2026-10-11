package commerce.novamart.order.service;

import commerce.novamart.common.util.Dates;
import commerce.novamart.order.dto.OrderDtos.OrderItemResponse;
import commerce.novamart.order.dto.OrderDtos.OrderResponse;
import commerce.novamart.order.dto.OrderDtos.TimelineEntry;
import commerce.novamart.order.entity.Order;
import commerce.novamart.order.entity.OrderEvent;
import commerce.novamart.order.entity.OrderStatus;
import commerce.novamart.user.dto.UserDtos.AddressResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OrderMapper {

    private static final Map<String, String> LABELS = Map.of(
            "placed", "Order placed",
            "payment-confirmed", "Payment confirmed",
            "processing", "Processing",
            "packed", "Packed",
            "shipped", "Shipped",
            "out-for-delivery", "Out for delivery",
            "delivered", "Delivered",
            "cancelled", "Cancelled",
            "returned", "Returned");

    public OrderResponse toResponse(Order o) {
        List<OrderItemResponse> items = o.getItems().stream()
                .map(i -> new OrderItemResponse(i.getId().toString(),
                        i.getProduct() == null ? null : i.getProduct().getId().toString(),
                        i.getProduct() == null ? null : i.getProduct().getSlug(),
                        i.getName(), i.getImage(), i.getPrice(), i.getQuantity(), i.getSelectedVariants()))
                .toList();

        AddressResponse address = new AddressResponse(o.getId().toString(), o.getShipFullName(), o.getShipPhone(),
                o.getShipEmail(), o.getShipCountry(), o.getShipProvince(), o.getShipCity(), o.getShipDistrict(),
                o.getShipStreet(), o.getShipPostalCode(), false, null);

        return new OrderResponse(o.getId().toString(), o.getOrderNumber(), Dates.iso(o.getCreatedAt()),
                o.getStatus().api(), items, o.getSubtotal(), o.getDiscount(), o.getShipping(), o.getTax(), o.getTotal(),
                address, o.getPaymentMethod(), o.getShippingMethod(), timeline(o), o.getTrackingNumber(),
                o.getCouponCode(), o.getContactEmail(), o.getUser() == null ? null : o.getUser().getId().toString());
    }

    private List<TimelineEntry> timeline(Order o) {
        Map<String, OrderEvent> byStatus = new java.util.HashMap<>();
        o.getEvents().forEach(e -> byStatus.put(e.getStatus(), e));

        List<TimelineEntry> entries = new ArrayList<>();
        for (String step : OrderStatus.STEPS) {
            OrderEvent e = byStatus.get(step);
            entries.add(new TimelineEntry(step, LABELS.get(step), e == null ? "" : Dates.iso(e.getOccurredAt()), e != null));
        }
        for (String terminal : List.of("cancelled", "returned")) {
            OrderEvent e = byStatus.get(terminal);
            if (e != null) entries.add(new TimelineEntry(terminal, LABELS.get(terminal), Dates.iso(e.getOccurredAt()), true));
        }
        return entries;
    }
}
