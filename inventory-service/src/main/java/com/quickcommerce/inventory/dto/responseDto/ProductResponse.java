package com.quickcommerce.inventory.dto.responseDto;

public record ProductResponse(long id, String sku, String name, long priceInPaise, int available) {
}
