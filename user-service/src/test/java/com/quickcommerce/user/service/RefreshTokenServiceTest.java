package com.quickcommerce.user.service;

import com.quickcommerce.user.exception.InvalidTokenException;
import com.quickcommerce.user.model.RefreshToken;
import com.quickcommerce.user.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repo;

    @Captor
    private ArgumentCaptor<RefreshToken> tokenCaptor;

    private Clock clock;
    private RefreshTokenService service;
    private Instant future;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneOffset.UTC);
        service = new RefreshTokenService(repo, clock, 7);
        future = Instant.parse("2026-11-20T00:00:00Z");
    }

    private RefreshToken stored(String raw, Instant expiresAt) {
        RefreshToken token = new RefreshToken(5L, RefreshTokenService.hash(raw), expiresAt);
        when(repo.findByTokenHash(RefreshTokenService.hash(raw))).thenReturn(Optional.of(token));
        return token;
    }

    @Test
    @DisplayName("issue() stores only the SHA-256 hash of the token, not the plaintext token")
    void issueStoresOnlyTheHashOfTheToken() {
        String raw = service.issue(5L);

        verify(repo).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();

        assertNotEquals(raw, savedToken.getTokenHash());
        assertEquals(RefreshTokenService.hash(raw), savedToken.getTokenHash());
        assertEquals(Instant.parse("2026-11-24T10:00:00Z"), savedToken.getExpiresAt());
    }

    @Test
    @DisplayName("rotate() revokes the old token and issues a new one")
    void rotateRevokesTheOldTokenAndIssuesANewOne() {
        RefreshToken old = stored("old-token", future);

        RefreshTokenService.Rotated result = service.rotate("old-token");

        assertTrue(old.isRevoked());
        assertEquals(5L, result.userId());
        assertNotEquals("old-token", result.refreshToken());
    }

    @Test
    @DisplayName("rotate() with a previously revoked token triggers token theft detection and revokes all tokens for user")
    void reusingARevokedTokenRevokesEverythingForTheUser() {
        RefreshToken used = stored("used-token", future);
        used.revoke(clock.instant());

        assertThrows(InvalidTokenException.class, () -> service.rotate("used-token"));
        verify(repo).revokeAllForUser(eq(5L), any(Instant.class));
    }

    @Test
    @DisplayName("rotate() with an expired token is rejected with InvalidTokenException")
    void expiredTokenIsRejected() {
        stored("expired-token", clock.instant().minusSeconds(1));
        assertThrows(InvalidTokenException.class, () -> service.rotate("expired-token"));
    }

    @Test
    @DisplayName("rotate() with an unknown token is rejected with InvalidTokenException")
    void unknownTokenIsRejected() {
        when(repo.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThrows(InvalidTokenException.class, () -> service.rotate("nope"));
    }
}
