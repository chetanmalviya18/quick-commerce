package com.quickcommerce.inventory.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisStockCounter implements StockCounter {
    private static final String PREFIX = "inventory:stock:";

    private final StringRedisTemplate redis;

    @Override
    public void set(long productId, int available) {
        redis.opsForValue().set(key(productId), String.valueOf(available));
    }

    @Override
    public Optional<Integer> get(long productId) {
        String value = redis.opsForValue().get(key(productId));
        return value == null ? Optional.empty() : Optional.of(Integer.parseInt(value));
    }

    @Override
    public void initializeIfAbsent(long productId, int available) {
        redis.opsForValue().setIfAbsent(key(productId), String.valueOf(available));
    }

    private static String key(long productId) {
        return PREFIX + productId;
    }
}
