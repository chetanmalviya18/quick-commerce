package com.quickcommerce.order.cart;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RedisCartStore implements CartStore{
    private static final String PREFIX = "cart:";

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public RedisCartStore(StringRedisTemplate redis, @Value("${app.cart.ttl-days}") long ttlDays) {
        this.redis = redis;
        this.ttl = Duration.ofDays(ttlDays);
    }

    @Override
    public Map<Long, Integer> get(long userId) {
        HashOperations<String, Object, Object> hash = redis.opsForHash();
        Map<Long, Integer> cart = new LinkedHashMap<>();
        hash.entries(key(userId)).forEach((productId, quantity) ->
                cart.put(Long.valueOf((String) productId), Integer.valueOf((String) quantity)));
        return cart;
    }

    @Override
    public void setQuantity(long userId, long productId, int quantity) {
        String key = key(userId);
        HashOperations<String, Object, Object> hash = redis.opsForHash();
        if (quantity == 0) {
            hash.delete(key, String.valueOf(productId));
            return;
        }
        hash.put(key, String.valueOf(productId), String.valueOf(quantity));
        redis.expire(key, ttl);
    }

    @Override
    public void clear(long userId) {
        redis.delete(key(userId));
    }

    private static String key(long userId) {
        return PREFIX + userId;
    }
}
