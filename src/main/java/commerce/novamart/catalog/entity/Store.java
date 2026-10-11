package commerce.novamart.catalog.entity;

import commerce.novamart.common.entity.BaseEntity;
import commerce.novamart.common.persistence.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
public class Store extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(nullable = false, length = 120)
    private String country;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(length = 200)
    private String hours;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private List<String> services = new ArrayList<>();

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;
}
