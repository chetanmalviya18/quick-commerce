package com.quickcommerce.inventory.service;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final ProductRepository productRepository;
    private final StockCounter stockCounter;
    private final StockAvailability stockAvailability;
    private final Clock clock;
    private final long holdMinutes;

    public ReservationService(ReservationRepository reservationRepository,
                              ProductRepository productRepository,
                              StockCounter stockCounter,
                              StockAvailability stockAvailability,
                              Clock clock, @Value("${app.inventory.hold-minutes}") long holdMinutes) {
        this.reservationRepository = reservationRepository;
        this.productRepository = productRepository;
        this.stockCounter = stockCounter;
        this.stockAvailability = stockAvailability;
        this.clock = clock;
        this.holdMinutes = holdMinutes;
    }

    @Transactional
    public ReservationResponse reserve(ReserveRequest request) {
        Optional<Reservation> existingReservation = reservationRepository.findByOrderId(request.orderId());
        if(existingReservation.isPresent()) {
            log.info("Reservation already exists for order {}", request.orderId());
            return toResponse(existingReservation.get());
        }

        Map<Long, Integer> quantities = merge(request.items());
        List<Product> foundProduct = productRepository.findAllByIdInAndActiveTrue(quantities.keySet());
        if(foundProduct.size() != quantities.size() || foundProduct.stream().anyMatch(p -> !p.isActive())) {
            throw new ProductNotFoundException();
        }

        // Self-healing: rebuild any counter that Redis lost before running the script.
        foundProduct.forEach(p -> stockCounter.initializeIfAbsent(p.getId(),
                stockAvailability.availableFor(p)));

        if(!stockCounter.tryReserve(quantities)) {
            log.info("Reservation refused for order {}: not enough stock", request.orderId());
            throw new InsufficientStockException();
        }
        try {
            Instant now = clock.instant();
            Reservation savedReservation = reservationRepository.saveAndFlush(new Reservation(request.orderId(),
                    toItems(quantities), now.plus(holdMinutes, ChronoUnit.MINUTES), now));
            log.info("Stock reserved for order {}: {} products, expires {}",
                    request.orderId(), quantities.size(), savedReservation.getExpiresAt());
            return toResponse(savedReservation);
        } catch (DataIntegrityViolationException e) {
            stockCounter.release(quantities);
            throw new DuplicateReservationException();
        } catch (RuntimeException e) {
            stockCounter.release(quantities);
            throw e;
        }
    }

    @Transactional(noRollbackFor = ReservationNotActiveException.class)
    public ReservationResponse confirm(String orderId) {
        Reservation reservation = find(orderId);
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            return toResponse(reservation);
        }
        if (reservation.getStatus() != ReservationStatus.HELD) {
            throw new ReservationNotActiveException();
        }
        for (ReservationItem item : reservation.getItems()) {
            if(productRepository.decrementOnHand(item.getProductId(), item.getQuantity()) == 0) {
                throw new IllegalStateException(
                        "On-hand stock is below the reserved quantity for product " + item.getProductId());
            }
        }
        reservation.confirm(clock.instant());
        reservationRepository.saveAndFlush(reservation);
        log.info("Reservation confirmed for order {}", orderId);
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse release(String orderId) {
        Reservation reservation = find(orderId);
        ReservationStatus status = reservation.getStatus();
        if (status == ReservationStatus.RELEASED || status == ReservationStatus.EXPIRED) {
            return toResponse(reservation);
        }
        if (status == ReservationStatus.CONFIRMED) {
            throw new ReservationNotActiveException();
        }
        reservation.release(clock.instant());
        stockCounter.release(quantitiesOf(reservation));
        log.info("Reservation released for order {}", orderId);
        return toResponse(reservation);
    }

    @Transactional
    public int expireOverdue() {
        Instant now = clock.instant();
        List<Reservation> overdue = reservationRepository
                .findTop100ByStatusAndExpiresAtBefore(ReservationStatus.HELD, now);

        for (Reservation reservation : overdue) {
            reservation.expire(now);
            stockCounter.release(quantitiesOf(reservation));
            log.info("Reservation expired for order {}", reservation.getOrderId());
        }
        return overdue.size();
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(String orderId) {
        return toResponse(find(orderId));
    }

    private static Map<Long, Integer> quantitiesOf(Reservation reservation) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        reservation.getItems().forEach(item -> quantities.put(item.getProductId(), item.getQuantity()));
        return quantities;
    }

    private Reservation find(String orderId) {
        return reservationRepository.findByOrderId(orderId).orElseThrow(ReservationNotFoundException::new);
    }

    private static List<ReservationItem> toItems(Map<Long, Integer> quantities) {
        return quantities.entrySet().stream()
                .map(entry -> new ReservationItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static Map<Long, Integer> merge(List<ReserveItem> items) {
        Map<Long, Integer> merged = new LinkedHashMap<>();
        items.forEach(item -> merged.merge(item.productId(), item.quantity(), Integer::sum));
        return merged;
    }

    private static ReservationResponse toResponse(Reservation reservation) {
        List<ReserveItem> items = reservation.getItems().stream()
                .map(item -> new ReserveItem(item.getProductId(), item.getQuantity()))
                .toList();
        return new ReservationResponse(reservation.getOrderId(), reservation.getStatus().name(),
                reservation.getExpiresAt(), items);
    }
}
