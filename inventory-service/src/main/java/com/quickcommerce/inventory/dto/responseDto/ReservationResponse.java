package com.quickcommerce.inventory.dto.responseDto;

import com.quickcommerce.inventory.dto.requestDto.ReserveItem;

import java.time.Instant;
import java.util.List;

public record ReservationResponse(String orderId, String status, Instant expiresAt, List<ReserveItem> items) {
}
