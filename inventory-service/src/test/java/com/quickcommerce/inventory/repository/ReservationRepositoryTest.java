package com.quickcommerce.inventory.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.quickcommerce.inventory.model.Reservation;
import com.quickcommerce.inventory.model.ReservationItem;
import com.quickcommerce.inventory.model.ReservationStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class ReservationRepositoryTest {
    @Autowired ReservationRepository reservations;

    private final Instant now = Instant.parse("2026-11-24T10:00:00Z");

    private Reservation reservation(String orderId, long productId, int quantity, Instant expiresAt) {
        return new Reservation(orderId, List.of(new ReservationItem(productId, quantity)), expiresAt, now);
    }

    @Test
    void findsAReservationByOrderIdTogetherWithItsItems() {
        reservations.save(reservation("order-1", 1L, 2, now.plus(10, ChronoUnit.MINUTES)));

        Reservation found = reservations.findByOrderId("order-1").orElseThrow();

        assertEquals(ReservationStatus.HELD, found.getStatus());
        assertEquals(1, found.getItems().size());
        assertEquals(2, found.getItems().get(0).getQuantity());
    }

    @Test
    void heldQuantityCountsOnlyHeldReservations() {
        reservations.save(reservation("order-1", 1L, 2, now.plus(10, ChronoUnit.MINUTES)));
        Reservation confirmed = reservations.save(reservation("order-2", 1L, 3, now.plus(10, ChronoUnit.MINUTES)));
        confirmed.confirm(now);
        reservations.save(confirmed);

        assertEquals(2L, reservations.heldQuantity(1L));
        assertEquals(0L, reservations.heldQuantity(99L));
    }

    @Test
    void findsOnlyOverdueHeldReservations() {
        reservations.save(reservation("overdue", 1L, 1, now.minus(1, ChronoUnit.MINUTES)));
        reservations.save(reservation("still-valid", 1L, 1, now.plus(1, ChronoUnit.MINUTES)));
        Reservation alreadyExpired = reservations.save(reservation("expired", 1L, 1, now.minus(1, ChronoUnit.MINUTES)));
        alreadyExpired.expire(now);
        reservations.save(alreadyExpired);

        List<Reservation> overdue = reservations.findTop100ByStatusAndExpiresAtBefore(ReservationStatus.HELD, now);

        assertEquals(1, overdue.size());
        assertEquals("overdue", overdue.get(0).getOrderId());
    }

    @Test
    void rejectsASecondReservationForTheSameOrder() {
        reservations.saveAndFlush(reservation("order-1", 1L, 1, now.plus(10, ChronoUnit.MINUTES)));
        assertThrows(DataIntegrityViolationException.class,
                () -> reservations.saveAndFlush(reservation("order-1", 2L, 1, now.plus(10, ChronoUnit.MINUTES))));
    }
}