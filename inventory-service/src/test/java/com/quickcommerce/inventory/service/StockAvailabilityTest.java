package com.quickcommerce.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.quickcommerce.inventory.model.Product;
import com.quickcommerce.inventory.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class StockAvailabilityTest {
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final StockAvailability availability = new StockAvailability(reservations);

    private Product product(long id, int onHand) {
        Product p = new Product("SKU-" + id, "Name", 100, onHand);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    void availableIsOnHandMinusWhatIsHeld() {
        when(reservations.heldQuantity(3L)).thenReturn(15L);
        assertEquals(25, availability.availableFor(product(3L, 40)));
    }

    @Test
    void availableNeverGoesBelowZero() {
        when(reservations.heldQuantity(3L)).thenReturn(50L);
        assertEquals(0, availability.availableFor(product(3L, 40)));
    }
}