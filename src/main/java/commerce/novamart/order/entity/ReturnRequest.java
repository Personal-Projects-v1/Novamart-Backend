package commerce.novamart.order.entity;

import commerce.novamart.common.entity.BaseEntity;
import commerce.novamart.common.persistence.StringListConverter;
import commerce.novamart.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "return_requests")
@Getter
@Setter
@NoArgsConstructor
public class ReturnRequest extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 120)
    private String reason;

    @Column(columnDefinition = "text")
    private String details;

    /** Ids of the order items being returned. */
    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private List<String> itemIds = new ArrayList<>();

    /** "original" (back to the payment method) or "store-credit". */
    @Column(nullable = false, length = 30)
    private String refundMethod = "original";

    @Column(nullable = false, length = 30)
    private String status = "requested";
}
