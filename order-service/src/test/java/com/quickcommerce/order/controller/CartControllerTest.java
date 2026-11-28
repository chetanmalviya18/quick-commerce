package com.quickcommerce.order.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.quickcommerce.order.config.SecurityConfig;
import com.quickcommerce.order.dto.responsetDto.CartItem;
import com.quickcommerce.order.dto.responsetDto.CartResponse;
import com.quickcommerce.order.exception.InvalidQuantityException;
import com.quickcommerce.order.exception.InventoryUnavailableException;
import com.quickcommerce.order.exception.ProductNotFoundException;
import com.quickcommerce.order.service.CartService;
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

@WebMvcTest(value = CartController.class,
        properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
@Import(SecurityConfig.class)
class CartControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CartService service;
    @MockitoBean JwtDecoder jwtDecoder;

    private static final String ITEM = """
            {"productId":10,"quantity":3}
            """;

    private JwtRequestPostProcessor customer(String userId) {
        return jwt().jwt(j -> j.subject(userId)).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    @Test
    void withoutATokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
    }

    @Test
    void anAdminCannotUseTheCart() throws Exception {
        mvc.perform(get("/api/v1/cart").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void aCustomerSeesTheirOwnCart() throws Exception {
        when(service.get(5L)).thenReturn(new CartResponse(List.of(new CartItem(10L, 3))));

        mvc.perform(get("/api/v1/cart").with(customer("5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(10))
                .andExpect(jsonPath("$.items[0].quantity").value(3));
    }

    @Test
    void settingAnItemUsesTheUserIdFromTheToken() throws Exception {
        when(service.setItem(5L, 10L, 3)).thenReturn(new CartResponse(List.of(new CartItem(10L, 3))));

        mvc.perform(put("/api/v1/cart/items").with(customer("5"))
                        .contentType(MediaType.APPLICATION_JSON).content(ITEM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3));
    }

    @Test
    void aMissingProductIdReturns400() throws Exception {
        mvc.perform(put("/api/v1/cart/items").with(customer("5"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"quantity":3}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anUnknownProductReturns404() throws Exception {
        when(service.setItem(anyLong(), anyLong(), anyInt())).thenThrow(new ProductNotFoundException());
        mvc.perform(put("/api/v1/cart/items").with(customer("5"))
                        .contentType(MediaType.APPLICATION_JSON).content(ITEM))
                .andExpect(status().isNotFound());
    }

    @Test
    void aBadQuantityReturns400() throws Exception {
        when(service.setItem(anyLong(), anyLong(), anyInt())).thenThrow(new InvalidQuantityException());
        mvc.perform(put("/api/v1/cart/items").with(customer("5"))
                        .contentType(MediaType.APPLICATION_JSON).content(ITEM))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenInventoryIsDownTheAnswerIs503() throws Exception {
        when(service.setItem(anyLong(), anyLong(), anyInt())).thenThrow(new InventoryUnavailableException());
        mvc.perform(put("/api/v1/cart/items").with(customer("5"))
                        .contentType(MediaType.APPLICATION_JSON).content(ITEM))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void clearingTheCartReturns204() throws Exception {
        mvc.perform(delete("/api/v1/cart").with(customer("5"))).andExpect(status().isNoContent());
        verify(service).clear(5L);
    }
}