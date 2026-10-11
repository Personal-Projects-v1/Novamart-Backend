package commerce.novamart.catalog.entity;

import commerce.novamart.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "brands")
@Getter
@Setter
@NoArgsConstructor
public class Brand extends BaseEntity {

    @Column(nullable = false, unique = true, length = 160)
    private String slug;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 500)
    private String logo;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean active = true;
}
