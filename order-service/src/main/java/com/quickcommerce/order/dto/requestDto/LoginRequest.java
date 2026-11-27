package com.quickcommerce.order.dto.requestDto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String email,
                           @NotBlank String password) {
}
