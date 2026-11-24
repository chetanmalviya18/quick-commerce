package com.quickcommerce.inventory.repository;

import com.quickcommerce.inventory.model.Reservation;
import com.quickcommerce.inventory.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByOrderId(String orderId);

    List<Reservation> findTop100ByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);

    @Query("select coalesce(sum(i.quantity), 0) from Reservation r join r.items i "
            + "where r.status = com.quickcommerce.inventory.model.ReservationStatus.HELD "
            + "and i.productId = :productId")
    long heldQuantity(@Param("productId") long productId);
}
