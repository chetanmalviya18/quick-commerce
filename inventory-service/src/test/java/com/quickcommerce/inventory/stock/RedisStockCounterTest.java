package com.quickcommerce.inventory.stock;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
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

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void tryReserveRunsTheScriptOnAllProductKeysAndReportsSuccess() {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        quantities.put(1L, 2);
        quantities.put(2L, 1);
        when(redis.execute(any(RedisScript.class), eq(List.of("inventory:stock:1", "inventory:stock:2")),
                eq("2"), eq("1"))).thenReturn(1L);

        assertTrue(counter.tryReserve(quantities));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void tryReserveReturnsFalseWhenTheScriptRefuses() {
        when(redis.execute(any(RedisScript.class), eq(List.of("inventory:stock:1")), eq("5"))).thenReturn(0L);

        assertFalse(counter.tryReserve(Map.of(1L, 5)));
    }

    @Test
    void releaseAddsTheQuantitiesBack() {
        counter.release(Map.of(5L, 3));
        verify(ops).increment("inventory:stock:5", 3L);
    }
}
