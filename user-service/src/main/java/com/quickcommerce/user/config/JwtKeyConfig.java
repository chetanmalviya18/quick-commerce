package com.quickcommerce.user.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.util.Base64;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class JwtKeyConfig {

    @Bean
    public RSAKey rsaKey(@Value("${app.jwt.private-key-location:}") String privateLocation,
                         @Value("${app.jwt.public-key-location:}") String publicLocation,
                         ResourceLoader resourceLoader) throws Exception {
        RSAPublicKey publicKey;
        RSAPrivateKey privateKey;

        if(privateLocation.isBlank() || publicLocation.isBlank()) {
            log.warn("No signing key configured: using a temporary key pair. " +
                    "Tokens stop working when this service restarts.");

            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            publicKey = (RSAPublicKey) pair.getPublic();
            privateKey = (RSAPrivateKey) pair.getPrivate();
        } else {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            privateKey = (RSAPrivateKey) factory.generatePrivate(
                    new PKCS8EncodedKeySpec(readPem(resourceLoader.getResource(privateLocation))));
            publicKey = (RSAPublicKey) factory.generatePublic(
                    new X509EncodedKeySpec(readPem(resourceLoader.getResource(publicLocation))));
        }

        return new RSAKey.Builder(publicKey).privateKey(privateKey).keyIDFromThumbprint().build();
    }

    private static byte[] readPem(Resource resource) throws IOException {
        String body = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                .lines()
                .filter(line -> !line.startsWith("-----"))
                .map(String::trim)
                .collect(Collectors.joining());
        return Base64.getDecoder().decode(body);
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey key) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey key) throws JOSEException {
        return NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
