package com.quickcommerce.inventory.stock;

import java.util.Optional;

public interface StockCounter {
    /** Sets the available quantity, replacing any existing value. */
    void set(long productId, int available);

    /** Returns the available quantity, or empty if Redis has no counter for the product. */
    Optional<Integer> get(long productId);

    /** Sets the counter only if none exists yet. Used for warm-up. */
    void initializeIfAbsent(long productId, int available);
}
