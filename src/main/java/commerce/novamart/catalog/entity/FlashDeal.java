package commerce.novamart.catalog.entity;

import commerce.novamart.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "flash_deals")
@Getter
@Setter
@NoArgsConstructor
public class FlashDeal extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private LocalDateTime endsAt;

    @Column(nullable = false)
    private int claimedPercent;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal dealPrice;

    @Column(nullable = false)
    private boolean active = true;
}
