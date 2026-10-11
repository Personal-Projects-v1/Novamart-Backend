package commerce.novamart.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import commerce.novamart.catalog.entity.Brand;
import commerce.novamart.catalog.entity.Category;
import commerce.novamart.catalog.entity.FlashDeal;
import commerce.novamart.catalog.entity.Product;
import commerce.novamart.catalog.entity.ProductCollection;
import commerce.novamart.catalog.entity.Store;
import commerce.novamart.catalog.repository.BrandRepository;
import commerce.novamart.catalog.repository.CategoryRepository;
import commerce.novamart.catalog.repository.FlashDealRepository;
import commerce.novamart.catalog.repository.ProductCollectionRepository;
import commerce.novamart.catalog.repository.ProductRepository;
import commerce.novamart.catalog.repository.StoreRepository;
import commerce.novamart.common.persistence.Json;
import commerce.novamart.common.util.Slugs;
import commerce.novamart.coupon.entity.Coupon;
import commerce.novamart.coupon.repository.CouponRepository;
import commerce.novamart.notification.entity.Notification;
import commerce.novamart.notification.repository.NotificationRepository;
import commerce.novamart.order.entity.Order;
import commerce.novamart.order.entity.OrderItem;
import commerce.novamart.order.entity.OrderStatus;
import commerce.novamart.order.repository.OrderRepository;
import commerce.novamart.order.service.OrderService;
import commerce.novamart.review.entity.ProductQuestion;
import commerce.novamart.review.entity.QuestionAnswer;
import commerce.novamart.review.entity.Review;
import commerce.novamart.review.repository.QuestionRepository;
import commerce.novamart.review.repository.ReviewRepository;
import commerce.novamart.user.entity.Address;
import commerce.novamart.user.entity.Role;
import commerce.novamart.user.entity.User;
import commerce.novamart.user.repository.AddressRepository;
import commerce.novamart.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the demo storefront (the same catalog the frontend used to mock) the first time the app starts
 * against an empty database. Enabled by {@code app.seed.enabled}, which only the dev profile turns on.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    // ----- seed file shapes ---------------------------------------------------------------------
    record SBrand(String slug, String name, String logo, String description, Boolean featured) {}
    record SCategory(String slug, String name, String description, String image, String icon, String parentSlug) {}
    record SProduct(String slug, String sku, String name, String brandSlug, String categorySlug, List<String> images,
                    String thumbnail, BigDecimal price, BigDecimal compareAtPrice, String currency, int stock,
                    List<String> badges, String shortDescription, String description, JsonNode variants,
                    JsonNode specGroups, List<String> tags, Boolean featured, Boolean bestSeller, Boolean freeShipping,
                    Integer warrantyMonths, List<String> colorSwatches, int createdDaysAgo, BigDecimal seedRating,
                    int seedReviewCount) {}
    record SCollection(String slug, String title, String description, String image, List<String> productSlugs) {}
    record SDeal(String productSlug, double endsInHours, int claimedPercent, BigDecimal dealPrice) {}
    record SCoupon(String code, String type, BigDecimal value, BigDecimal minSubtotal, int expiresInDays, String description) {}
    record SStore(String name, String city, String country, String address, String hours, List<String> services,
                  double lat, double lng) {}
    record SReview(String productSlug, String userName, int rating, String title, String body, boolean verifiedPurchase,
                   int helpfulCount, int daysAgo) {}
    record SAnswer(String body, String answeredBy, @JsonProperty("isStore") boolean isStore, int helpfulCount, int daysAgo) {}
    record SQuestion(String productSlug, String question, String askedBy, int daysAgo, List<SAnswer> answers) {}
    record SOrderItem(String productSlug, int quantity) {}
    record SOrder(String status, int daysAgo, String paymentMethod, String shippingMethod, String trackingNumber,
                  List<SOrderItem> items) {}

    private final BrandRepository brands;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final ProductCollectionRepository collections;
    private final FlashDealRepository deals;
    private final CouponRepository coupons;
    private final StoreRepository stores;
    private final ReviewRepository reviews;
    private final QuestionRepository questions;
    private final UserRepository users;
    private final AddressRepository addresses;
    private final OrderRepository orders;
    private final NotificationRepository notifications;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final EntityManager em;

    @Value("${app.seed.enabled:false}")
    private boolean enabled;
    @Value("${app.seed.admin-email:}")
    private String adminEmail;
    @Value("${app.seed.admin-password:}")
    private String adminPassword;
    @Value("${app.seed.customer-email:}")
    private String customerEmail;
    @Value("${app.seed.customer-password:}")
    private String customerPassword;

    /** created_at values to rewrite once everything is flushed (auditing stamps "now" on insert). */
    private final List<Runnable> backdates = new ArrayList<>();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        User admin = ensureUser(adminEmail, adminPassword, "Nova", "Admin", Role.ADMIN, "+250788000001");
        User customer = ensureUser(customerEmail, customerPassword, "Isaac", "P.", Role.CUSTOMER, "+250799401377");
        if (brands.count() > 0) {
            refreshPlaceholderImages();
            log.info("Catalog already present - skipping demo data seed");
            return;
        }
        log.info("Seeding demo catalog...");

        Map<String, Brand> brandBySlug = new HashMap<>();
        for (SBrand b : read("brands", new TypeReference<List<SBrand>>() {})) {
            Brand e = new Brand();
            e.setSlug(b.slug());
            e.setName(b.name());
            e.setLogo(blankToNull(b.logo()));
            e.setDescription(b.description());
            e.setFeatured(Boolean.TRUE.equals(b.featured()));
            brandBySlug.put(b.slug(), brands.save(e));
        }

        Map<String, Category> categoryBySlug = new HashMap<>();
        List<SCategory> categorySeeds = read("categories", new TypeReference<List<SCategory>>() {});
        for (SCategory c : categorySeeds) {
            Category e = new Category();
            e.setSlug(c.slug());
            e.setName(c.name());
            e.setDescription(c.description());
            e.setImage(c.image());
            e.setIcon(c.icon());
            if (c.parentSlug() != null) e.setParent(categoryBySlug.get(c.parentSlug()));
            categoryBySlug.put(c.slug(), categories.save(e));
        }

        Map<String, Product> productBySlug = new HashMap<>();
        for (SProduct p : read("products", new TypeReference<List<SProduct>>() {})) {
            Product e = new Product();
            e.setSlug(p.slug());
            e.setSku(p.sku());
            e.setName(p.name());
            e.setBrand(brandBySlug.get(p.brandSlug()));
            e.setCategory(categoryBySlug.get(p.categorySlug()));
            e.setImages(p.images());
            e.setThumbnail(p.thumbnail());
            e.setPrice(p.price());
            e.setCompareAtPrice(p.compareAtPrice());
            e.setCurrency(p.currency() == null ? "USD" : p.currency());
            e.setRating(p.seedRating());
            e.setReviewCount(p.seedReviewCount());
            e.setStock(p.stock());
            e.setBadges(p.badges());
            e.setShortDescription(p.shortDescription());
            e.setDescription(p.description());
            e.setVariants(p.variants());
            e.setSpecGroups(p.specGroups());
            e.setTags(p.tags());
            e.setFeatured(Boolean.TRUE.equals(p.featured()));
            e.setBestSeller(Boolean.TRUE.equals(p.bestSeller()));
            e.setFreeShipping(Boolean.TRUE.equals(p.freeShipping()));
            e.setWarrantyMonths(p.warrantyMonths() == null ? 12 : p.warrantyMonths());
            e.setColorSwatches(p.colorSwatches() == null ? List.of() : p.colorSwatches());
            e.setColors(colorLabels(p.variants()));
            e.setSearchText(searchText(e));
            productBySlug.put(p.slug(), products.save(e));
            backdate("products", e, p.createdDaysAgo() * 24);
        }

        for (SCollection c : read("collections", new TypeReference<List<SCollection>>() {})) {
            ProductCollection e = new ProductCollection();
            e.setSlug(c.slug());
            e.setTitle(c.title());
            e.setDescription(c.description());
            e.setImage(c.image());
            e.setProducts(new ArrayList<>(c.productSlugs().stream().map(productBySlug::get).toList()));
            collections.save(e);
        }

        for (SDeal d : read("flash-deals", new TypeReference<List<SDeal>>() {})) {
            FlashDeal e = new FlashDeal();
            e.setProduct(productBySlug.get(d.productSlug()));
            e.setEndsAt(LocalDateTime.now().plusMinutes(Math.round(d.endsInHours() * 60)));
            e.setClaimedPercent(d.claimedPercent());
            e.setDealPrice(d.dealPrice());
            deals.save(e);
        }

        for (SCoupon c : read("coupons", new TypeReference<List<SCoupon>>() {})) {
            Coupon e = new Coupon();
            e.setCode(c.code());
            e.setType(Coupon.Type.valueOf(c.type().toUpperCase(Locale.ROOT)));
            e.setValue(c.value());
            e.setMinSubtotal(c.minSubtotal());
            e.setExpiresAt(LocalDateTime.now().plusDays(c.expiresInDays()));
            e.setDescription(c.description());
            coupons.save(e);
        }

        for (SStore s : read("stores", new TypeReference<List<SStore>>() {})) {
            Store e = new Store();
            e.setName(s.name());
            e.setCity(s.city());
            e.setCountry(s.country());
            e.setAddress(s.address());
            e.setHours(s.hours());
            e.setServices(s.services());
            e.setLat(s.lat());
            e.setLng(s.lng());
            stores.save(e);
        }

        for (SReview r : read("reviews", new TypeReference<List<SReview>>() {})) {
            Review e = new Review();
            e.setProduct(productBySlug.get(r.productSlug()));
            e.setUserName(r.userName());
            e.setRating(r.rating());
            e.setTitle(r.title());
            e.setBody(r.body());
            e.setVerifiedPurchase(r.verifiedPurchase());
            e.setHelpfulCount(r.helpfulCount());
            reviews.save(e);
            backdate("reviews", e, r.daysAgo() * 24);
        }

        for (SQuestion q : read("questions", new TypeReference<List<SQuestion>>() {})) {
            ProductQuestion e = new ProductQuestion();
            e.setProduct(productBySlug.get(q.productSlug()));
            e.setAskedBy(q.askedBy());
            e.setQuestion(q.question());
            for (SAnswer a : q.answers()) {
                QuestionAnswer ans = new QuestionAnswer();
                ans.setQuestion(e);
                ans.setAnsweredBy(a.answeredBy());
                ans.setBody(a.body());
                ans.setStore(a.isStore());
                ans.setHelpfulCount(a.helpfulCount());
                e.getAnswers().add(ans);
            }
            questions.save(e);
            backdate("product_questions", e, q.daysAgo() * 24);
            for (QuestionAnswer ans : e.getAnswers()) backdate("question_answers", ans, Math.max(0, q.daysAgo() - 1) * 24);
        }

        seedCustomerData(customer, productBySlug);

        em.flush();
        backdates.forEach(Runnable::run);
        log.info("Demo data seeded: {} brands, {} categories, {} products", brands.count(), categories.count(), products.count());
    }

    // ----- image refresh --------------------------------------------------------------------------

    private static boolean isPlaceholder(String url) {
        return url != null && url.contains("picsum.photos");
    }

    /**
     * Databases seeded by an earlier version carry random placeholder photos (picsum.photos). Swap those for the
     * curated images in the seed files - and only those: anything an admin has edited is left alone.
     */
    private void refreshPlaceholderImages() {
        int changed = 0;
        for (SBrand b : read("brands", new TypeReference<List<SBrand>>() {})) {
            var brand = brands.findBySlug(b.slug()).orElse(null);
            if (brand != null && isPlaceholder(brand.getLogo())) {
                brand.setLogo(blankToNull(b.logo()));
                changed++;
            }
        }
        for (SCategory c : read("categories", new TypeReference<List<SCategory>>() {})) {
            var category = categories.findBySlug(c.slug()).orElse(null);
            if (category != null && isPlaceholder(category.getImage())) {
                category.setImage(c.image());
                changed++;
            }
        }
        for (SCollection c : read("collections", new TypeReference<List<SCollection>>() {})) {
            var collection = collections.findBySlug(c.slug()).orElse(null);
            if (collection != null && isPlaceholder(collection.getImage())) {
                collection.setImage(c.image());
                changed++;
            }
        }
        for (SProduct p : read("products", new TypeReference<List<SProduct>>() {})) {
            var product = products.findBySlug(p.slug()).orElse(null);
            if (product != null && (isPlaceholder(product.getThumbnail()) || product.getImages().stream().anyMatch(DataSeeder::isPlaceholder))) {
                product.setImages(new ArrayList<>(p.images()));
                product.setThumbnail(p.thumbnail());
                changed++;
            }
        }
        em.flush();
        // order history keeps its own image snapshot; refresh the placeholder ones too
        jdbc.update("update order_items oi set image = p.thumbnail from products p "
                + "where oi.product_id = p.id and oi.image like '%picsum.photos%'");
        if (changed > 0) log.info("Replaced placeholder images on {} catalog records", changed);
    }

    // ----- customer demo data -------------------------------------------------------------------

    private void seedCustomerData(User customer, Map<String, Product> productBySlug) {
        Address home = address(customer, "Home", "+250 788 123 456", "Gasabo", "KG 7 Ave, House 12", "00000", true);
        address(customer, "Office", "+250 788 999 222", "Kicukiro", "KK 15 Rd, Office 4", null, false);

        int index = 0;
        for (SOrder so : read("orders", new TypeReference<List<SOrder>>() {})) {
            Order o = new Order();
            o.setUser(customer);
            o.setContactEmail(customer.getEmail());
            o.setContactPhone(home.getPhone());
            o.setPaymentMethod(index % 2 == 0 ? "Credit / Debit Card" : "Mobile Money");
            o.setShippingMethod(so.shippingMethod());
            o.setTrackingNumber(so.trackingNumber());
            o.setShipFullName(home.getFullName());
            o.setShipPhone(home.getPhone());
            o.setShipEmail(customer.getEmail());
            o.setShipCountry(home.getCountry());
            o.setShipProvince(home.getProvince());
            o.setShipCity(home.getCity());
            o.setShipDistrict(home.getDistrict());
            o.setShipStreet(home.getStreet());
            o.setShipPostalCode(home.getPostalCode());

            BigDecimal subtotal = BigDecimal.ZERO;
            for (SOrderItem si : so.items()) {
                Product p = productBySlug.get(si.productSlug());
                OrderItem item = new OrderItem();
                item.setProduct(p);
                item.setName(p.getName());
                item.setImage(p.getThumbnail());
                item.setPrice(p.getPrice());
                item.setQuantity(si.quantity());
                o.addItem(item);
                subtotal = subtotal.add(p.getPrice().multiply(BigDecimal.valueOf(si.quantity())));
            }
            BigDecimal discount = index % 3 == 0 ? subtotal.multiply(new BigDecimal("0.10")).setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
            String methodKey = so.shippingMethod().startsWith("Express") ? "express" : "standard";
            var t = OrderService.totals(subtotal, discount, methodKey);
            o.setSubtotal(t.subtotal());
            o.setDiscount(t.discount());
            o.setShipping(t.shipping());
            o.setTax(t.tax());
            o.setTotal(t.total());
            o.setOrderNumber("VX-" + (480000 + index));
            OrderStatus status = OrderStatus.valueOf(so.status().toUpperCase(Locale.ROOT).replace('-', '_'));
            o.setStatus(status);

            LocalDateTime placedAt = LocalDateTime.now().minusDays(so.daysAgo());
            int reached = status.stepIndex() < 0 ? 2 : status.stepIndex();
            for (int i = 0; i <= reached; i++) o.addEvent(OrderStatus.STEPS.get(i), placedAt.plusDays(i));
            if (status == OrderStatus.CANCELLED || status == OrderStatus.RETURNED) o.addEvent(status.api(), placedAt.plusDays(3));

            orders.save(o);
            backdate("orders", o, so.daysAgo() * 24L);
            index++;
        }

        notification(customer, "order", "Your order has shipped", "Order VX-480001 is on its way and should arrive in 3-5 days.", "/account/orders", 1, false);
        notification(customer, "price-drop", "Price drop on your wishlist item", "SoundWave Pro Wireless Earbuds just dropped in price.", "/wishlist", 5, false);
        notification(customer, "promotion", "Weekend flash sale is live", "Up to 40% off audio and gaming gear, for 48 hours only.", "/deals", 26, true);
        notification(customer, "stock", "Back in stock", "Nebula 27 Gaming Monitor is back in stock.", "/wishlist", 50, true);
        notification(customer, "security", "New sign-in detected", "We noticed a new sign-in to your account.", "/account/security", 96, true);
    }

    private Address address(User user, String label, String phone, String district, String street, String postal, boolean def) {
        Address a = new Address();
        a.setUser(user);
        a.setLabel(label);
        a.setFullName(user.getFirstName() + " " + user.getLastName());
        a.setPhone(phone);
        a.setEmail(user.getEmail());
        a.setCountry("Rwanda");
        a.setProvince("Kigali City");
        a.setCity("Kigali");
        a.setDistrict(district);
        a.setStreet(street);
        a.setPostalCode(postal);
        a.setDefaultAddress(def);
        return addresses.save(a);
    }

    private void notification(User user, String type, String title, String body, String href, int hoursAgo, boolean read) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setHref(href);
        n.setRead(read);
        notifications.save(n);
        backdate("notifications", n, hoursAgo);
    }

    // ----- helpers ------------------------------------------------------------------------------

    private User ensureUser(String email, String password, String first, String last, Role role, String phone) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("app.seed.* credentials must be configured when app.seed.enabled=true");
        }
        return users.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setFirstName(first);
            u.setLastName(last);
            u.setEmail(email);
            u.setPhone(phone);
            u.setPassword(passwordEncoder.encode(password));
            u.setRole(role);
            u.setEnabled(true);
            u.setEmailVerified(true);
            u.setPhoneVerified(false);
            return users.save(u);
        });
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }

    private static java.util.Set<String> colorLabels(JsonNode variants) {
        java.util.Set<String> labels = new java.util.LinkedHashSet<>();
        if (variants != null && variants.isArray()) {
            for (JsonNode group : variants) {
                if ("Color".equals(group.path("name").asString(""))) {
                    for (JsonNode option : group.path("options")) {
                        String label = option.path("label").asString("");
                        if (!label.isBlank()) labels.add(label);
                    }
                }
            }
        }
        return labels;
    }

    private static String searchText(Product p) {
        return (p.getName() + " " + p.getBrand().getName() + " " + p.getCategory().getName() + " " + String.join(" ", p.getTags()))
                .toLowerCase(Locale.ROOT);
    }

    private void backdate(String table, commerce.novamart.common.entity.BaseEntity entity, long hoursAgo) {
        backdates.add(() -> {
            Timestamp ts = Timestamp.valueOf(LocalDateTime.now().minusHours(hoursAgo));
            jdbc.update("update " + table + " set created_at = ?, updated_at = ? where id = ?", ts, ts, entity.getId());
        });
    }

    private <T> T read(String name, TypeReference<T> type) {
        try (InputStream in = new ClassPathResource("seed/" + name + ".json").getInputStream()) {
            return Json.MAPPER.readValue(in, type);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read seed file " + name, e);
        }
    }
}
