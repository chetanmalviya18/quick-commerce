package com.quickcommerce.inventory.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class ReservationSweeperTest {
    @Test
    void sweepAsksTheServiceToExpireOverdueReservations() {
        ReservationService service = mock(ReservationService.class);
        when(service.expireOverdue()).thenReturn(2);

        new ReservationSweeper(service).sweep();

        verify(service).expireOverdue();
    }
}