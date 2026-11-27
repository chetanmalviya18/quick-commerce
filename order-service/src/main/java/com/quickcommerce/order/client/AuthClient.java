package com.quickcommerce.order.client;

import com.quickcommerce.order.dto.requestDto.LoginRequest;
import com.quickcommerce.order.dto.responsetDto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service")
public interface AuthClient {

    @PostMapping("/api/v1/auth/login")
    TokenResponse login(@Valid @RequestBody LoginRequest loginRequest);
}
