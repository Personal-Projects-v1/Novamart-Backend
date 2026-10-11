-- ============================================================
-- V2: commerce domain (catalog, cart, wishlist, orders, reviews, ...)
-- JSON-ish payloads are stored as TEXT and (de)serialised by JPA converters.
-- ============================================================

-- User profile / communication preferences ----------------------------------
ALTER TABLE users ADD COLUMN birthday DATE;
ALTER TABLE users ADD COLUMN gender VARCHAR(20);
ALTER TABLE users ADD COLUMN pref_order_updates BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN pref_promotions BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN pref_price_drops BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN pref_newsletter BOOLEAN NOT NULL DEFAULT FALSE;

-- OTP codes are stored hashed (BCrypt, 60 chars), which does not fit V1's VARCHAR(10)
ALTER TABLE otp_tokens ALTER COLUMN otp TYPE VARCHAR(100);

-- Catalog -------------------------------------------------------------------
CREATE TABLE brands (
    id UUID PRIMARY KEY,
    slug VARCHAR(160) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    logo VARCHAR(500),
    description TEXT,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE categories (
    id UUID PRIMARY KEY,
    slug VARCHAR(160) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    image VARCHAR(500),
    icon VARCHAR(60),
    parent_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    slug VARCHAR(220) NOT NULL UNIQUE,
    sku VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    brand_id UUID NOT NULL REFERENCES brands(id),
    category_id UUID NOT NULL REFERENCES categories(id),
    thumbnail VARCHAR(500),
    price NUMERIC(12,2) NOT NULL,
    compare_at_price NUMERIC(12,2),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    rating NUMERIC(3,2) NOT NULL DEFAULT 0,
    review_count INTEGER NOT NULL DEFAULT 0,
    stock INTEGER NOT NULL DEFAULT 0,
    short_description TEXT,
    description TEXT,
    warranty_months INTEGER NOT NULL DEFAULT 12,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    best_seller BOOLEAN NOT NULL DEFAULT FALSE,
    free_shipping BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    images TEXT NOT NULL DEFAULT '[]',
    badges TEXT NOT NULL DEFAULT '[]',
    variants TEXT NOT NULL DEFAULT '[]',
    spec_groups TEXT NOT NULL DEFAULT '[]',
    tags TEXT NOT NULL DEFAULT '[]',
    color_swatches TEXT NOT NULL DEFAULT '[]',
    search_text TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_products_brand ON products(brand_id);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_price ON products(price);

CREATE TABLE product_colors (
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    label VARCHAR(80) NOT NULL,
    PRIMARY KEY (product_id, label)
);

CREATE TABLE collections (
    id UUID PRIMARY KEY,
    slug VARCHAR(160) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    image VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE collection_products (
    collection_id UUID NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    position INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (collection_id, product_id)
);

CREATE TABLE flash_deals (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    ends_at TIMESTAMP NOT NULL,
    claimed_percent INTEGER NOT NULL DEFAULT 0,
    deal_price NUMERIC(12,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE coupons (
    id UUID PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    value NUMERIC(12,2) NOT NULL,
    min_subtotal NUMERIC(12,2),
    expires_at TIMESTAMP NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    usage_limit INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE stores (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    city VARCHAR(120) NOT NULL,
    country VARCHAR(120) NOT NULL,
    address VARCHAR(300) NOT NULL,
    hours VARCHAR(200),
    services TEXT NOT NULL DEFAULT '[]',
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Customer data -------------------------------------------------------------
CREATE TABLE addresses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label VARCHAR(60),
    full_name VARCHAR(200) NOT NULL,
    phone VARCHAR(40) NOT NULL,
    email VARCHAR(255),
    country VARCHAR(120) NOT NULL,
    province VARCHAR(120) NOT NULL,
    city VARCHAR(120) NOT NULL,
    district VARCHAR(120),
    street VARCHAR(300) NOT NULL,
    postal_code VARCHAR(30),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_addresses_user ON addresses(user_id);

CREATE TABLE cart_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL,
    selected_variants TEXT NOT NULL DEFAULT '{}',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_cart_items_user ON cart_items(user_id);

CREATE TABLE wishlist_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (user_id, product_id)
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    href VARCHAR(300),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_notifications_user ON notifications(user_id);

-- Orders --------------------------------------------------------------------
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(30) NOT NULL UNIQUE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(40),
    status VARCHAR(30) NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL,
    discount NUMERIC(12,2) NOT NULL DEFAULT 0,
    shipping NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax NUMERIC(12,2) NOT NULL DEFAULT 0,
    total NUMERIC(12,2) NOT NULL,
    coupon_code VARCHAR(60),
    payment_method VARCHAR(60) NOT NULL,
    shipping_method VARCHAR(60) NOT NULL,
    tracking_number VARCHAR(80),
    ship_full_name VARCHAR(200) NOT NULL,
    ship_phone VARCHAR(40) NOT NULL,
    ship_email VARCHAR(255),
    ship_country VARCHAR(120) NOT NULL,
    ship_province VARCHAR(120) NOT NULL,
    ship_city VARCHAR(120) NOT NULL,
    ship_district VARCHAR(120),
    ship_street VARCHAR(300) NOT NULL,
    ship_postal_code VARCHAR(30),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    image VARCHAR(500),
    price NUMERIC(12,2) NOT NULL,
    quantity INTEGER NOT NULL,
    selected_variants TEXT NOT NULL DEFAULT '{}'
);
CREATE INDEX idx_order_items_order ON order_items(order_id);

CREATE TABLE order_events (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    occurred_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_order_events_order ON order_events(order_id);

CREATE TABLE return_requests (
    id UUID PRIMARY KEY,
    reference VARCHAR(30) NOT NULL UNIQUE,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    reason VARCHAR(120) NOT NULL,
    details TEXT,
    item_ids TEXT NOT NULL DEFAULT '[]',
    refund_method VARCHAR(30) NOT NULL DEFAULT 'original',
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Reviews & Q&A -------------------------------------------------------------
CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    user_name VARCHAR(160) NOT NULL,
    rating INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    images TEXT NOT NULL DEFAULT '[]',
    verified_purchase BOOLEAN NOT NULL DEFAULT FALSE,
    helpful_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_reviews_product ON reviews(product_id);
CREATE INDEX idx_reviews_user ON reviews(user_id);

CREATE TABLE product_questions (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    asked_by VARCHAR(160) NOT NULL,
    question TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_questions_product ON product_questions(product_id);

CREATE TABLE question_answers (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES product_questions(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    answered_by VARCHAR(160) NOT NULL,
    body TEXT NOT NULL,
    is_store BOOLEAN NOT NULL DEFAULT FALSE,
    helpful_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_answers_question ON question_answers(question_id);
