package com.quickcommerce.order.controller;

import com.quickcommerce.order.dto.requestDto.SetCartItemRequest;
import com.quickcommerce.order.dto.responsetDto.CartResponse;
import com.quickcommerce.order.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {
    private final CartService service;

    @GetMapping
    public ResponseEntity<CartResponse> get(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.get(userId(jwt)));
    }

    @PutMapping("/items")
    public ResponseEntity<CartResponse> setItem(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody SetCartItemRequest request) {
        return ResponseEntity.ok(service.setItem(userId(jwt), request.productId(), request.quantity()));
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(@AuthenticationPrincipal Jwt jwt) {
        service.clear(userId(jwt));
        return ResponseEntity.noContent().build();
    }

    private static long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
