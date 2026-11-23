package com.quickcommerce.inventory.dto.requestDto;

import jakarta.validation.constraints.Min;

public record UpdateStockRequest (
        @Min(0) int quantity
) {
}
