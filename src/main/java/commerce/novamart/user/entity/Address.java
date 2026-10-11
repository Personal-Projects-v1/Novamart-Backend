package commerce.novamart.user.entity;

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

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
public class Address extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 60)
    private String label;

    @Column(nullable = false, length = 200)
    private String fullName;

    @Column(nullable = false, length = 40)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 120)
    private String country;

    @Column(nullable = false, length = 120)
    private String province;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(length = 120)
    private String district;

    @Column(nullable = false, length = 300)
    private String street;

    @Column(length = 30)
    private String postalCode;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;
}
