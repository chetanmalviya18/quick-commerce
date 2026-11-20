package com.quickcommerce.user.controller;

import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwksControllerTest {

    @Test
    @DisplayName("GET /.well-known/jwks.json returns public key in JWKS format with 200 OK")
    void jwksReturnsPublicKeySet() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        var pair = gen.generateKeyPair();
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate())
                .keyID("test-key-id")
                .build();

        JwksController controller = new JwksController(rsaKey);
        ResponseEntity<Map<String, Object>> response = controller.jwks();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("keys"));
        assertNotNull(response.getHeaders().getCacheControl());
    }
}
