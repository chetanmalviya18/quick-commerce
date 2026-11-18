package com.quickcommerce.user.service;

import com.quickcommerce.user.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceMockitoTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Test
    void issueAccessTokenEncodesExpectedClaimsUsingMockito() {
        Instant fixedInstant = Instant.parse("2026-01-01T12:00:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

        TokenService service = new TokenService(jwtEncoder, fixedClock, "mock-issuer", 15);

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("mocked.jwt.token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        String token = service.issueAccessToken(123L, Role.CUSTOMER);

        assertEquals("mocked.jwt.token", token);

        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder, times(1)).encode(captor.capture());

        JwtEncoderParameters params = captor.getValue();
        assertEquals("mock-issuer", params.getClaims().getClaimAsString("iss"));
        assertEquals("123", params.getClaims().getSubject());
        assertEquals(List.of("CUSTOMER"), params.getClaims().getClaim("roles"));
    }

    @Test
    void expiresInSecondsCalculatesCorrectly() {
        TokenService service = new TokenService(jwtEncoder, Clock.systemUTC(), "mock-issuer", 30);
        assertEquals(1800, service.expiresInSeconds());
    }
}
