package com.quickcommerce.user.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.quickcommerce.user.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TokenServiceTest {
    private final RSAKey rsaKey = newKey();
    private final TokenService  tokenService = new TokenService(
            new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey))),
            Clock.systemUTC(), "test-issuer", 15
    );

    private static RSAKey newKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("test-key").generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void tokenCarriesSubjectRoleIssuerAndExpiry() throws Exception {
        String token = tokenService.issueAccessToken(42L, Role.RIDER);
        Jwt jwt = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build().decode(token);

        assertEquals("42", jwt.getSubject());
        assertEquals("test-issuer", jwt.getClaimAsString("iss"));
        assertEquals(List.of("RIDER"), jwt.getClaimAsStringList("roles"));
        assertEquals(Duration.ofMinutes(15), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
    }

    @Test
    void tokenIsRejectedByAnotherPublicKey() throws Exception {
        String token = tokenService.issueAccessToken(42L, Role.CUSTOMER);
        var otherDecoder = NimbusJwtDecoder.withPublicKey(newKey().toRSAPublicKey()).build();
        assertThrows(JwtException.class, () -> otherDecoder.decode(token));
    }
}
