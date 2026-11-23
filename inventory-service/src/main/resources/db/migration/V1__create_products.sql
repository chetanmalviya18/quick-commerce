CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sku VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    price_in_paise BIGINT NOT NULL,
    on_hand INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT chk_products_on_hand CHECK (on_hand >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;