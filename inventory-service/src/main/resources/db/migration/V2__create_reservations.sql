CREATE TABLE reservations (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              order_id VARCHAR(64) NOT NULL,
                              status VARCHAR(20) NOT NULL,
                              expires_at DATETIME(6) NOT NULL,
                              created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                              updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                              PRIMARY KEY (id),
                              CONSTRAINT uk_reservations_order UNIQUE (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_reservations_status_expiry ON reservations (status, expires_at);

CREATE TABLE reservation_items (
                                   reservation_id BIGINT NOT NULL,
                                   product_id BIGINT NOT NULL,
                                   quantity INT NOT NULL,
                                   PRIMARY KEY (reservation_id, product_id),
                                   CONSTRAINT fk_items_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
                                   CONSTRAINT chk_items_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;