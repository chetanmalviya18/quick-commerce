package com.quickcommerce.user.dto.responseDto;

public record TokenResponse(String accessToken,
                            String refreshToken,
                            String tokenType,
                            long expiresIn) {
}
