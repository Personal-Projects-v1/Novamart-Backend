package commerce.novamart.user.entity;

import commerce.novamart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_phone", columnList = "phone")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(nullable = false)
    private String password;

    private String avatarUrl;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Column(nullable = false)
    private Boolean phoneVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CUSTOMER;

    private LocalDate birthday;

    @Column(length = 20)
    private String gender;

    @Column(name = "pref_order_updates", nullable = false)
    private boolean prefOrderUpdates = true;

    @Column(name = "pref_promotions", nullable = false)
    private boolean prefPromotions = true;

    @Column(name = "pref_price_drops", nullable = false)
    private boolean prefPriceDrops = true;

    @Column(name = "pref_newsletter", nullable = false)
    private boolean prefNewsletter = false;
}
