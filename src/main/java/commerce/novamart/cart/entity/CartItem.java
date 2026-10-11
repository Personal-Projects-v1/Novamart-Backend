package commerce.novamart.cart.entity;

import commerce.novamart.catalog.entity.Product;
import commerce.novamart.common.entity.BaseEntity;
import commerce.novamart.common.persistence.StringMapConverter;
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

import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Convert(converter = StringMapConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private Map<String, String> selectedVariants = new LinkedHashMap<>();
}
