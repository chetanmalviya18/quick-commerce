package com.quickcommerce.user.controller;

import com.quickcommerce.user.dto.responseDto.UserResponse;
import com.quickcommerce.user.exception.GlobalExceptionHandler;
import com.quickcommerce.user.exception.UserNotFoundException;
import com.quickcommerce.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/users/me returns user profile successfully")
    void meSuccess() {
        Jwt jwt = new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(900),
                Map.of("alg", "RS256"), Map.of("sub", "5"));

        when(userService.getUserProfile(5L)).thenReturn(new UserResponse(5L, "user@example.com", "CUSTOMER"));

        ResponseEntity<UserResponse> res = userController.me(jwt);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(5L, res.getBody().id());
        assertEquals("user@example.com", res.getBody().email());
        assertEquals("CUSTOMER", res.getBody().role());
    }

    @Test
    @DisplayName("GET /api/v1/users/me propagates UserNotFoundException when user is not found")
    void meUserNotFound() {
        Jwt jwt = new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(900),
                Map.of("alg", "RS256"), Map.of("sub", "99"));

        when(userService.getUserProfile(99L)).thenThrow(new UserNotFoundException());

        assertThrows(UserNotFoundException.class, () -> userController.me(jwt));
    }
}
