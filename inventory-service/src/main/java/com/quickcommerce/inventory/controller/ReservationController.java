package com.quickcommerce.inventory.controller;

import com.quickcommerce.inventory.dto.requestDto.ReserveRequest;
import com.quickcommerce.inventory.dto.responseDto.ReservationResponse;
import com.quickcommerce.inventory.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SERVICE')")
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReserveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.reserve(request));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ReservationResponse> get(@PathVariable String orderId) {
        return ResponseEntity.ok(reservationService.get(orderId));
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ReservationResponse> confirm(@PathVariable String orderId) {
        return ResponseEntity.ok(reservationService.confirm(orderId));
    }

    @PostMapping("/{orderId}/release")
    public ResponseEntity<ReservationResponse> release(@PathVariable String orderId) {
        return ResponseEntity.ok(reservationService.release(orderId));
    }
}
