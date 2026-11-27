package com.quickcommerce.order.service;

import com.quickcommerce.order.cart.CartStore;
import com.quickcommerce.order.client.InventoryGateway;
import com.quickcommerce.order.dto.responsetDto.CartItem;
import com.quickcommerce.order.dto.responsetDto.CartResponse;
import com.quickcommerce.order.exception.CartFullException;
import com.quickcommerce.order.exception.InvalidQuantityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CartService {
    private final CartStore store;
    private final InventoryGateway inventory;
    private final int maxQuantity;
    private final int maxItems;

    public CartService(CartStore store, InventoryGateway inventory,
                       @Value("${app.cart.max-quantity-per-item}") int maxQuantity,
                       @Value("${app.cart.max-items}") int maxItems) {
        this.store = store;
        this.inventory = inventory;
        this.maxQuantity = maxQuantity;
        this.maxItems = maxItems;
    }

    public CartResponse get(long userId) {
        return toResponse(store.get(userId));
    }

    public CartResponse setItem(long userId, long productId, int quantity) {
        if (quantity < 0 || quantity > maxQuantity) {
            throw new InvalidQuantityException();
        }
        if (quantity == 0) {
            store.setQuantity(userId, productId, 0);
            return get(userId);
        }
        Map<Long, Integer> current = store.get(userId);
        if (!current.containsKey(productId) && current.size() >= maxItems) {
            throw new CartFullException();
        }
        inventory.getProduct(productId);
        store.setQuantity(userId, productId, quantity);
        log.info("Cart updated: user id={}, product id={}, quantity={}", userId, productId, quantity);
        return get(userId);
    }

    public void clear(long userId) {
        store.clear(userId);
    }

    private static CartResponse toResponse(Map<Long, Integer> cart) {
        List<CartItem> items = cart.entrySet().stream()
                .map(entry -> new CartItem(entry.getKey(), entry.getValue()))
                .toList();
        return new CartResponse(items);
    }

}
