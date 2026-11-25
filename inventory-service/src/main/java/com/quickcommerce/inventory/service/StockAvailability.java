package com.quickcommerce.inventory.service;

import com.quickcommerce.inventory.model.Product;
import com.quickcommerce.inventory.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockAvailability {
    private final ReservationRepository reservations;

    /** On-hand stock minus the quantity held by active reservations, never below zero. */
    public int availableFor(Product product) {
        long held = reservations.heldQuantity(product.getId());
        return (int) Math.max(0, product.getOnHand() - held);
    }
}
