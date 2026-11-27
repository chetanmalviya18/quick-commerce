package com.quickcommerce.order.dto.responsetDto;

public record ProductInfo(long id, String sku, String name, long priceInPaise, int available) {}
