package com.quickcommerce.order.cart;

import java.util.Map;

public interface CartStore {
    /** Product ID to quantity, in the order the products were added. */
    Map<Long, Integer> get(long userId);

    /** Sets the quantity of one product. A quantity of 0 removes the product. */
    void setQuantity(long userId, long productId, int quantity);

    void clear(long userId);
}
