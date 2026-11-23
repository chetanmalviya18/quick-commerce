package com.quickcommerce.inventory.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_in_paise", nullable = false)
    private long priceInPaise;

    @Column(name = "on_hand", nullable = false)
    private int onHand;

    @Column(nullable = false)
    private boolean active = true;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Product(String sku, String name, long priceInPaise, int onHand) {
        this.sku = sku;
        this.name = name;
        this.priceInPaise = priceInPaise;
        this.onHand = onHand;
    }

    public void changeStock(int newOnHand) {
        this.onHand = newOnHand;
    }
}
