package com.quickcommerce.inventory.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reservation_items", joinColumns = @JoinColumn(name = "reservation_id"))
    private List<ReservationItem> items = new ArrayList<>();

    public Reservation(String orderId, List<ReservationItem> items, Instant expiresAt, Instant now) {
        this.orderId = orderId;
        this.items = new ArrayList<>(items);
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.HELD;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void confirm(Instant now) { change(ReservationStatus.CONFIRMED, now); }
    public void release(Instant now) { change(ReservationStatus.RELEASED, now); }
    public void expire(Instant now) { change(ReservationStatus.EXPIRED, now); }

    private void change(ReservationStatus newStatus, Instant now) {
        this.status = newStatus;
        this.updatedAt = now;
    }
}
