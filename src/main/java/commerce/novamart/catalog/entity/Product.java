package commerce.novamart.catalog.entity;

import commerce.novamart.common.entity.BaseEntity;
import commerce.novamart.common.persistence.JsonNodeConverter;
import commerce.novamart.common.persistence.StringListConverter;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product extends BaseEntity {

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(length = 500)
    private String thumbnail;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(precision = 12, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(nullable = false)
    private int reviewCount;

    @Column(nullable = false)
    private int stock;

    @Column(columnDefinition = "text")
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private int warrantyMonths = 12;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean bestSeller;

    @Column(nullable = false)
    private boolean freeShipping;

    @Column(nullable = false)
    private boolean active = true;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private List<String> images = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private List<String> badges = new ArrayList<>();

    @Convert(converter = JsonNodeConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private JsonNode variants;

    @Convert(converter = JsonNodeConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private JsonNode specGroups;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private List<String> tags = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private List<String> colorSwatches = new ArrayList<>();

    /** Lower-cased "name brand category tags" blob used by free-text search. */
    @Column(nullable = false, columnDefinition = "text")
    private String searchText = "";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_colors", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "label", nullable = false, length = 80)
    private Set<String> colors = new LinkedHashSet<>();
}
