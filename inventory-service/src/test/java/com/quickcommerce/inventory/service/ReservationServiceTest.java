package com.quickcommerce.inventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.quickcommerce.inventory.dto.requestDto.ReserveItem;
import com.quickcommerce.inventory.dto.requestDto.ReserveRequest;
import com.quickcommerce.inventory.dto.responseDto.ReservationResponse;
import com.quickcommerce.inventory.exception.*;
import com.quickcommerce.inventory.model.Product;
import com.quickcommerce.inventory.model.Reservation;
import com.quickcommerce.inventory.model.ReservationItem;
import com.quickcommerce.inventory.model.ReservationStatus;
import com.quickcommerce.inventory.repository.ProductRepository;
import com.quickcommerce.inventory.repository.ReservationRepository;
import com.quickcommerce.inventory.stock.StockCounter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

class ReservationServiceTest {
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final StockCounter counter = mock(StockCounter.class);
    private final StockAvailability availability = mock(StockAvailability.class);
    private final Instant now = Instant.parse("2026-11-24T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final ReservationService service =
            new ReservationService(reservations, products, counter, availability, clock, 10);

    private Product product(long id) {
        Product p = new Product("SKU-" + id, "Name", 100, 50);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private Reservation held(String orderId, long productId, int quantity) {
        return new Reservation(orderId, List.of(new ReservationItem(productId, quantity)),
                now.plus(10, ChronoUnit.MINUTES), now);
    }

    private Map<Long, Integer> quantities(long productId, int quantity) {
        Map<Long, Integer> map = new LinkedHashMap<>();
        map.put(productId, quantity);
        return map;
    }

    @Test
    void reserveCreatesAHeldReservationThatExpiresAfterTheHoldTime() {
        when(reservations.findByOrderId("order-1")).thenReturn(Optional.empty());
        when(products.findAllByIdInAndActiveTrue(any())).thenReturn(List.of(product(1L), product(2L)));
        when(counter.tryReserve(any())).thenReturn(true);
        when(reservations.saveAndFlush(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse res = service.reserve(new ReserveRequest("order-1",
                List.of(new ReserveItem(1L, 2), new ReserveItem(2L, 1))));

        assertEquals("HELD", res.status());
        assertEquals(now.plus(10, ChronoUnit.MINUTES), res.expiresAt());
        Map<Long, Integer> expected = new LinkedHashMap<>();
        expected.put(1L, 2);
        expected.put(2L, 1);
        verify(counter).tryReserve(expected);
    }

    @Test
    void reserveMergesTheSameProductListedTwice() {
        when(reservations.findByOrderId("order-1")).thenReturn(Optional.empty());
        when(products.findAllByIdInAndActiveTrue(any())).thenReturn(List.of(product(1L)));
        when(counter.tryReserve(any())).thenReturn(true);
        when(reservations.saveAndFlush(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.reserve(new ReserveRequest("order-1",
                List.of(new ReserveItem(1L, 2), new ReserveItem(1L, 3))));

        verify(counter).tryReserve(quantities(1L, 5));
    }

    @Test
    void reservingTheSameOrderAgainReturnsTheExistingReservation() {
        when(reservations.findByOrderId("order-1")).thenReturn(Optional.of(held("order-1", 1L, 2)));

        ReservationResponse res = service.reserve(new ReserveRequest("order-1", List.of(new ReserveItem(1L, 2))));

        assertEquals("HELD", res.status());
        verifyNoInteractions(counter);
        verify(reservations, never()).saveAndFlush(any());
    }

    @Test
    void reserveIsRefusedWhenThereIsNotEnoughStock() {
        when(reservations.findByOrderId("order-1")).thenReturn(Optional.empty());
        when(products.findAllByIdInAndActiveTrue(any())).thenReturn(List.of(product(1L)));
        when(counter.tryReserve(any())).thenReturn(false);

        assertThrows(InsufficientStockException.class,
                () -> service.reserve(new ReserveRequest("order-1", List.of(new ReserveItem(1L, 99)))));
        verify(reservations, never()).saveAndFlush(any());
    }

    @Test
    void reserveIsRefusedForAnUnknownProduct() {
        when(reservations.findByOrderId("order-1")).thenReturn(Optional.empty());
        when(products.findAllByIdInAndActiveTrue(any())).thenReturn(List.of());

        assertThrows(ProductNotFoundException.class,
                () -> service.reserve(new ReserveRequest("order-1", List.of(new ReserveItem(7L, 1)))));
        verify(counter, never()).tryReserve(any());
    }

    @Test
    void reserveReturnsTheStockToRedisWhenTheDatabaseFails() {
        when(reservations.findByOrderId("order-1")).thenReturn(Optional.empty());
        when(products.findAllByIdInAndActiveTrue(any())).thenReturn(List.of(product(1L)));
        when(counter.tryReserve(any())).thenReturn(true);
        when(reservations.saveAndFlush(any(Reservation.class))).thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class,
                () -> service.reserve(new ReserveRequest("order-1", List.of(new ReserveItem(1L, 2)))));
        verify(counter).release(quantities(1L, 2));
    }

    @Test
    void reserveTurnsARaceOnTheSameOrderIntoAConflictAndReturnsTheStock() {
        when(reservations.findByOrderId("order-1")).thenReturn(Optional.empty());
        when(products.findAllByIdInAndActiveTrue(any())).thenReturn(List.of(product(1L)));
        when(counter.tryReserve(any())).thenReturn(true);
        when(reservations.saveAndFlush(any(Reservation.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(DuplicateReservationException.class,
                () -> service.reserve(new ReserveRequest("order-1", List.of(new ReserveItem(1L, 2)))));
        verify(counter).release(quantities(1L, 2));
    }

    @Test
    void confirmDecrementsOnHandAndMarksTheReservationConfirmed() {
        when(reservations.findByOrderId("order-1")).thenReturn(Optional.of(held("order-1", 1L, 2)));
        when(products.decrementOnHand(1L, 2)).thenReturn(1);

        ReservationResponse res = service.confirm("order-1");

        assertEquals("CONFIRMED", res.status());
        verify(products).decrementOnHand(1L, 2);
    }

    @Test
    void confirmingTwiceDoesNothingTheSecondTime() {
        Reservation reservation = held("order-1", 1L, 2);
        reservation.confirm(now);
        when(reservations.findByOrderId("order-1")).thenReturn(Optional.of(reservation));

        assertEquals("CONFIRMED", service.confirm("order-1").status());
        verify(products, never()).decrementOnHand(anyLong(), anyInt());
    }

    @Test
    void confirmAfterReleaseIsRefused() {
        Reservation reservation = held("order-1", 1L, 2);
        reservation.release(now);
        when(reservations.findByOrderId("order-1")).thenReturn(Optional.of(reservation));

        assertThrows(ReservationNotActiveException.class, () -> service.confirm("order-1"));
    }

    @Test
    void confirmForAnUnknownOrderIsNotFound() {
        when(reservations.findByOrderId("nope")).thenReturn(Optional.empty());
        assertThrows(ReservationNotFoundException.class, () -> service.confirm("nope"));
    }

    @Test
    void releaseMarksTheReservationReleasedAndReturnsTheStock() {
        when(reservations.findByOrderId("order-1")).thenReturn(Optional.of(held("order-1", 1L, 2)));

        ReservationResponse res = service.release("order-1");

        assertEquals("RELEASED", res.status());
        verify(counter).release(quantities(1L, 2));
    }

    @Test
    void releasingTwiceReturnsTheStockOnlyOnce() {
        Reservation reservation = held("order-1", 1L, 2);
        reservation.release(now);
        when(reservations.findByOrderId("order-1")).thenReturn(Optional.of(reservation));

        assertEquals("RELEASED", service.release("order-1").status());
        verify(counter, never()).release(any());
    }

    @Test
    void aConfirmedReservationCannotBeReleased() {
        Reservation reservation = held("order-1", 1L, 2);
        reservation.confirm(now);
        when(reservations.findByOrderId("order-1")).thenReturn(Optional.of(reservation));

        assertThrows(ReservationNotActiveException.class, () -> service.release("order-1"));
    }

    @Test
    void expireOverdueReleasesTheStockOfEveryOverdueReservation() {
        Reservation first = held("order-1", 1L, 2);
        Reservation second = held("order-2", 2L, 1);
        when(reservations.findTop100ByStatusAndExpiresAtBefore(ReservationStatus.HELD, now))
                .thenReturn(List.of(first, second));

        int expired = service.expireOverdue();

        assertEquals(2, expired);
        assertEquals(ReservationStatus.EXPIRED, first.getStatus());
        assertEquals(ReservationStatus.EXPIRED, second.getStatus());
        verify(counter, times(2)).release(any());
    }

    @Test
    void getReturnsTheReservation() {
        when(reservations.findByOrderId("order-1")).thenReturn(Optional.of(held("order-1", 1L, 2)));
        assertEquals("order-1", service.get("order-1").orderId());
    }
}