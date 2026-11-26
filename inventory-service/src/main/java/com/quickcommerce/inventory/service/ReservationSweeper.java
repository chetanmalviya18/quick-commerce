package com.quickcommerce.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReservationSweeper {
    private final ReservationService reservationService;

    @Scheduled(fixedDelayString = "${app.inventory.sweep-interval-ms:30000}")
    public void sweep() {
        int expired = reservationService.expireOverdue();
        if (expired > 0) {
            log.info("Expired {} overdue reservations", expired);
        }
    }
}
