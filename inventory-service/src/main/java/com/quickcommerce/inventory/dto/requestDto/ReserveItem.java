package com.quickcommerce.inventory.dto.requestDto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReserveItem(@NotNull Long productId, @Min(1) int quantity) {}
