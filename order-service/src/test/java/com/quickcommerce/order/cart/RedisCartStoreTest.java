package com.quickcommerce.order.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisCartStoreTest {
    @SuppressWarnings("unchecked")
    private final HashOperations<String, Object, Object> hash = mock(HashOperations.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final RedisCartStore store;

    RedisCartStoreTest() {
        doReturn(hash).when(redis).opsForHash();
        store = new RedisCartStore(redis, 7);
    }

    @Test
    void getReturnsProductIdsAndQuantities() {
        Map<Object, Object> entries = new LinkedHashMap<>();
        entries.put("10", "2");
        entries.put("11", "3");
        when(hash.entries("cart:5")).thenReturn(entries);

        Map<Long, Integer> cart = store.get(5L);

        assertEquals(2, cart.get(10L));
        assertEquals(3, cart.get(11L));
    }

    @Test
    void setQuantityStoresTheValueAndRestartsTheExpiry() {
        store.setQuantity(5L, 10L, 3);

        verify(hash).put("cart:5", "10", "3");
        verify(redis).expire("cart:5", Duration.ofDays(7));
    }

    @Test
    void aQuantityOfZeroRemovesTheProduct() {
        store.setQuantity(5L, 10L, 0);

        verify(hash).delete("cart:5", "10");
        verify(hash, never()).put(any(), any(), any());
    }

    @Test
    void clearDeletesTheWholeCart() {
        store.clear(5L);
        verify(redis).delete("cart:5");
    }
}