package com.quickcommerce.order.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;

import com.quickcommerce.order.dto.requestDto.LoginRequest;
import com.quickcommerce.order.dto.responsetDto.TokenResponse;
import org.junit.jupiter.api.Test;

class ServiceTokenProviderTest {
    private final AuthClient auth = mock(AuthClient.class);
    private final Clock clock = mock(Clock.class);
    private final Instant t0 = Instant.parse("2026-11-26T10:00:00Z");
    private final ServiceTokenProvider provider =
            new ServiceTokenProvider(auth, clock, "svc@internal.local", "secret");

    @Test
    void logsInOnFirstUseWithTheServiceCredentials() {
        when(clock.instant()).thenReturn(t0);
        when(auth.login(any())).thenReturn(new TokenResponse("token-1", "r", "Bearer", 900));

        assertEquals("token-1", provider.token());
        verify(auth).login(new LoginRequest("svc@internal.local", "secret"));
    }

    @Test
    void reusesTheTokenWhileItIsStillValid() {
        when(clock.instant()).thenReturn(t0, t0.plusSeconds(100));
        when(auth.login(any())).thenReturn(new TokenResponse("token-1", "r", "Bearer", 900));

        provider.token();
        assertEquals("token-1", provider.token());
        verify(auth, times(1)).login(any());
    }

    @Test
    void logsInAgainShortlyBeforeTheTokenExpires() {
        when(clock.instant()).thenReturn(t0, t0.plusSeconds(850));
        when(auth.login(any()))
                .thenReturn(new TokenResponse("token-1", "r", "Bearer", 900))
                .thenReturn(new TokenResponse("token-2", "r", "Bearer", 900));

        provider.token();
        assertEquals("token-2", provider.token());
        verify(auth, times(2)).login(any());
    }
}