package com.quickcommerce.inventory.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisStockCounter implements StockCounter {
    private static final String PREFIX = "inventory:stock:";
    private static final DefaultRedisScript<Long> RESERVE = reserveScript();

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

    @Override
    public boolean tryReserve(Map<Long, Integer> quantities) {
        List<String> keys = new ArrayList<>();
        List<String> amounts = new ArrayList<>();
        quantities.forEach((productId, quantity) -> {
            keys.add(key(productId));
            amounts.add(String.valueOf(quantity));
        });
        Long result = redis.execute(RESERVE, keys, amounts.toArray());
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public void release(Map<Long, Integer> quantities) {
        quantities.forEach((productId, quantity) -> {
            redis.opsForValue().increment(key(productId), quantity);
        });
    }

    private static DefaultRedisScript<Long> reserveScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("scripts/reserve.lua"));
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    private static String key(long productId) {
        return PREFIX + productId;
    }
}
