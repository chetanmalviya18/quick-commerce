package com.quickcommerce.order.dto.requestDto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SetCartItemRequest(@NotNull Long productId, @Min(0) int quantity) {}
