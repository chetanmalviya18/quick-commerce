package com.quickcommerce.order.dto.responsetDto;

public record TokenResponse(String accessToken,
                            String refreshToken,
                            String tokenType,
                            long expiresIn) {
}
