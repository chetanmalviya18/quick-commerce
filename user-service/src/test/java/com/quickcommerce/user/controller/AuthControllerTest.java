package com.quickcommerce.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcommerce.user.dto.requestDto.LoginRequest;
import com.quickcommerce.user.dto.requestDto.RefreshRequest;
import com.quickcommerce.user.dto.requestDto.RegisterRequest;
import com.quickcommerce.user.dto.responseDto.TokenResponse;
import com.quickcommerce.user.exception.EmailAlreadyUsedException;
import com.quickcommerce.user.exception.GlobalExceptionHandler;
import com.quickcommerce.user.exception.InvalidCredentialsException;
import com.quickcommerce.user.exception.InvalidTokenException;
import com.quickcommerce.user.model.Role;
import com.quickcommerce.user.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth - returns 201 Created on valid registration")
    void registerSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest("test@example.com", "password123", Role.CUSTOMER);
        TokenResponse res = new TokenResponse("access-token", "refresh-token", "Bearer", 900L);

        when(authService.register(any(RegisterRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("POST /api/v1/auth - returns 400 Bad Request with field validation errors when input is invalid")
    void registerValidationFailure() throws Exception {
        RegisterRequest req = new RegisterRequest("invalid-email", "short", Role.CUSTOMER);

        mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth"))
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("POST /api/v1/auth - returns 409 Conflict when email is already registered")
    void registerEmailAlreadyUsed() throws Exception {
        RegisterRequest req = new RegisterRequest("used@example.com", "password123", Role.CUSTOMER);

        when(authService.register(any())).thenThrow(new EmailAlreadyUsedException());

        mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email already registered"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - returns 401 Unauthorized when credentials are wrong")
    void loginWrongCredentials() throws Exception {
        LoginRequest req = new LoginRequest("test@example.com", "wrong-password");

        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Wrong email or password"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - returns 401 Unauthorized when token is invalid")
    void refreshInvalidToken() throws Exception {
        RefreshRequest req = new RefreshRequest("invalid-token");

        when(authService.refreshToken(any())).thenThrow(new InvalidTokenException());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - returns 204 No Content on logout")
    void logoutSuccess() throws Exception {
        RefreshRequest req = new RefreshRequest("valid-refresh-token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(authService).logout(any(RefreshRequest.class));
    }
}
