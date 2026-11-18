package com.quickcommerce.user.service;

import com.quickcommerce.user.exception.InvalidTokenException;
import com.quickcommerce.user.model.RefreshToken;
import com.quickcommerce.user.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
public class RefreshTokenService {
    public record Rotated(long userId, String refreshToken) {}

    private final RefreshTokenRepository repository;
    private final Clock clock;
    private final long days;
    private final SecureRandom  random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository, Clock clock,
                               @Value("${app.jwt.refresh-token-days}") long days) {
        this.repository = repository;
        this.clock = clock;
        this.days = days;
    }

    @Transactional
    public String issue(long userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().encodeToString(bytes);
        repository.save(new RefreshToken(userId, hash(raw), clock.instant().plus(days, ChronoUnit.DAYS)));
        log.debug("Refresh token issued for user {}", userId);
        return raw;
    }

    @Transactional(noRollbackFor = InvalidTokenException.class)
    public Rotated rotate(String raw) {
        RefreshToken token = repository.findByTokenHash(hash(raw)).orElseThrow(() -> {
            log.info("Refresh attempted with an unknown token");
            return new InvalidTokenException();
        });

        Instant now = clock.instant();
        if(token.isRevoked()) {
            log.warn("Refresh token reuse detected for user {}: all refresh tokens revoked",
                    token.getUserId());
            repository.revokeAllForUser(token.getUserId(), now);
            throw new InvalidTokenException();
        }
        if(token.getExpiresAt().isBefore(now)) {
            log.info("Expired refresh token presented for user {}", token.getUserId());
            throw new InvalidTokenException();
        }
        token.revoke(now);
        return new Rotated(token.getUserId(),  issue(token.getUserId()));
    }

    @Transactional
    public void revoke(String raw) {
        repository.findByTokenHash(hash(raw)).ifPresent(token -> {
            if (!token.isRevoked()) token.revoke(clock.instant());
        });
    }

    public static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
