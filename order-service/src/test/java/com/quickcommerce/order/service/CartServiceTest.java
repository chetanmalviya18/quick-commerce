package com.quickcommerce.order.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.quickcommerce.order.cart.CartStore;
import com.quickcommerce.order.client.InventoryGateway;
import com.quickcommerce.order.dto.responsetDto.CartResponse;
import com.quickcommerce.order.dto.responsetDto.ProductInfo;
import com.quickcommerce.order.exception.CartFullException;
import com.quickcommerce.order.exception.InvalidQuantityException;
import com.quickcommerce.order.exception.ProductNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CartServiceTest {
    private final CartStore store = mock(CartStore.class);
    private final InventoryGateway inventory = mock(InventoryGateway.class);
    private final CartService service = new CartService(store, inventory, 20, 2);

    private final ProductInfo milk = new ProductInfo(10L, "MILK-1L", "Milk 1L", 6500, 40);

    private Map<Long, Integer> cartOf(Object... pairs) {
        Map<Long, Integer> cart = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            cart.put((Long) pairs[i], (Integer) pairs[i + 1]);
        }
        return cart;
    }

    @Test
    void setItemChecksTheProductAndStoresTheQuantity() {
        when(inventory.getProduct(10L)).thenReturn(milk);
        when(store.get(5L)).thenReturn(cartOf(), cartOf(10L, 3));

        CartResponse cart = service.setItem(5L, 10L, 3);

        verify(store).setQuantity(5L, 10L, 3);
        assertEquals(1, cart.items().size());
        assertEquals(3, cart.items().get(0).quantity());
    }

    @Test
    void tooManyUnitsAreRejectedWithoutCallingInventory() {
        assertThrows(InvalidQuantityException.class, () -> service.setItem(5L, 10L, 21));
        verifyNoInteractions(inventory);
    }

    @Test
    void aNegativeQuantityIsRejected() {
        assertThrows(InvalidQuantityException.class, () -> service.setItem(5L, 10L, -1));
    }

    @Test
    void aQuantityOfZeroRemovesTheProductWithoutCallingInventory() {
        when(store.get(5L)).thenReturn(cartOf());

        service.setItem(5L, 10L, 0);

        verify(store).setQuantity(5L, 10L, 0);
        verifyNoInteractions(inventory);
    }

    @Test
    void anUnknownProductIsRejectedAndNothingIsStored() {
        when(store.get(5L)).thenReturn(cartOf());
        when(inventory.getProduct(10L)).thenThrow(new ProductNotFoundException());

        assertThrows(ProductNotFoundException.class, () -> service.setItem(5L, 10L, 1));
        verify(store, never()).setQuantity(anyLong(), anyLong(), anyInt());
    }

    @Test
    void aFullCartRejectsANewProduct() {
        when(store.get(5L)).thenReturn(cartOf(1L, 1, 2L, 1));

        assertThrows(CartFullException.class, () -> service.setItem(5L, 10L, 1));
        verifyNoInteractions(inventory);
    }

    @Test
    void aFullCartStillAllowsChangingAProductAlreadyInIt() {
        when(inventory.getProduct(1L)).thenReturn(new ProductInfo(1L, "A", "A", 100, 10));
        when(store.get(5L)).thenReturn(cartOf(1L, 1, 2L, 1));

        service.setItem(5L, 1L, 5);

        verify(store).setQuantity(5L, 1L, 5);
    }

    @Test
    void getReturnsTheItemsInTheCart() {
        when(store.get(5L)).thenReturn(cartOf(10L, 2, 11L, 4));

        CartResponse cart = service.get(5L);

        assertEquals(2, cart.items().size());
        assertEquals(11L, cart.items().get(1).productId());
    }

    @Test
    void clearEmptiesTheCart() {
        service.clear(5L);
        verify(store).clear(5L);
    }
}