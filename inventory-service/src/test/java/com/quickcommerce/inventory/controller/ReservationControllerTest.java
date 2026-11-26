package com.quickcommerce.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.quickcommerce.inventory.config.SecurityConfig;
import com.quickcommerce.inventory.dto.requestDto.ReserveItem;
import com.quickcommerce.inventory.dto.responseDto.ReservationResponse;
import com.quickcommerce.inventory.exception.InsufficientStockException;
import com.quickcommerce.inventory.exception.ReservationNotFoundException;
import com.quickcommerce.inventory.service.ReservationService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = ReservationController.class,
        properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
@Import(SecurityConfig.class)
class ReservationControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ReservationService service;
    @MockitoBean JwtDecoder jwtDecoder;

    private static final String BODY = """
            {"orderId":"order-1","items":[{"productId":1,"quantity":2}]}
            """;
    private static final ReservationResponse HELD = new ReservationResponse("order-1", "HELD",
            Instant.parse("2026-11-24T10:10:00Z"), List.of(new ReserveItem(1L, 2)));

    private JwtRequestPostProcessor as(String authority) {
        return jwt().authorities(new SimpleGrantedAuthority(authority));
    }

    @Test
    void withoutATokenReturns401() throws Exception {
        mvc.perform(post("/api/v1/reservations").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aCustomerIsForbidden() throws Exception {
        mvc.perform(post("/api/v1/reservations").with(as("ROLE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAdminCanReserve() throws Exception {
        when(service.reserve(any())).thenReturn(HELD);
        mvc.perform(post("/api/v1/reservations").with(as("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("HELD"))
                .andExpect(jsonPath("$.orderId").value("order-1"));
    }

    @Test
    void aServiceAccountCanReserve() throws Exception {
        when(service.reserve(any())).thenReturn(HELD);
        mvc.perform(post("/api/v1/reservations").with(as("ROLE_SERVICE"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void anEmptyItemListReturns400() throws Exception {
        mvc.perform(post("/api/v1/reservations").with(as("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"orderId":"order-1","items":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void notEnoughStockReturns409() throws Exception {
        when(service.reserve(any())).thenThrow(new InsufficientStockException());
        mvc.perform(post("/api/v1/reservations").with(as("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmAndReleaseReturn200() throws Exception {
        when(service.confirm("order-1")).thenReturn(new ReservationResponse("order-1", "CONFIRMED",
                Instant.parse("2026-11-24T10:10:00Z"), List.of(new ReserveItem(1L, 2))));
        when(service.release("order-1")).thenReturn(new ReservationResponse("order-1", "RELEASED",
                Instant.parse("2026-11-24T10:10:00Z"), List.of(new ReserveItem(1L, 2))));

        mvc.perform(post("/api/v1/reservations/order-1/confirm").with(as("ROLE_SERVICE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(post("/api/v1/reservations/order-1/release").with(as("ROLE_SERVICE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"));
    }

    @Test
    void anUnknownOrderReturns404() throws Exception {
        when(service.get("missing")).thenThrow(new ReservationNotFoundException());
        mvc.perform(get("/api/v1/reservations/missing").with(as("ROLE_ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void aCustomerCannotReadAReservation() throws Exception {
        mvc.perform(get("/api/v1/reservations/order-1").with(as("ROLE_CUSTOMER")))
                .andExpect(status().isForbidden());
    }
}