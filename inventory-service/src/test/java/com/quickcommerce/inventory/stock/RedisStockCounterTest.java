package com.quickcommerce.inventory.stock;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class RedisStockCounterTest {
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final RedisStockCounter counter;

    RedisStockCounterTest() {
        when(redis.opsForValue()).thenReturn(ops);
        counter = new RedisStockCounter(redis);
    }

    @Test
    void setWritesTheQuantityUnderThePrefixedKey() {
        counter.set(5L, 12);
        verify(ops).set("inventory:stock:5", "12");
    }

    @Test
    void getReturnsTheStoredQuantity() {
        when(ops.get("inventory:stock:5")).thenReturn("7");
        assertEquals(7, counter.get(5L).orElseThrow());
    }

    @Test
    void getReturnsEmptyWhenThereIsNoCounter() {
        when(ops.get("inventory:stock:9")).thenReturn(null);
        assertTrue(counter.get(9L).isEmpty());
    }

    @Test
    void initializeIfAbsentNeverOverwritesALiveCounter() {
        counter.initializeIfAbsent(5L, 40);
        verify(ops).setIfAbsent("inventory:stock:5", "40");
        verify(ops, never()).set(anyString(), anyString());
    }
}
