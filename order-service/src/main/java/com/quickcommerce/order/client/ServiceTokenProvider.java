package com.quickcommerce.order.client;

import com.quickcommerce.order.dto.requestDto.LoginRequest;
import com.quickcommerce.order.dto.responsetDto.TokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@Slf4j
public class ServiceTokenProvider {
    private final AuthClient authClient;
    private final Clock clock;
    private final String email;
    private final String password;

    private String token;
    private Instant refreshAt = Instant.MIN;

    public ServiceTokenProvider(AuthClient authClient, Clock clock,
                                @Value("${app.service-account.email}") String email,
                                @Value("${app.service-account.password}") String password) {
        this.authClient = authClient;
        this.clock = clock;
        this.email = email;
        this.password = password;
    }

    public synchronized String token() {
        Instant now = clock.instant();
        if(token == null || !now.isBefore(refreshAt)) {
            TokenResponse response = authClient.login(new LoginRequest(email, password));
            token = response.accessToken();
            refreshAt = now.plusSeconds(Math.max(0, response.expiresIn() - 60));
            log.info("Service token refreshed");
        }
        return token;
    }
}
