package com.quickcommerce.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.quickcommerce.inventory.config.SecurityConfig;
import com.quickcommerce.inventory.dto.responseDto.ProductResponse;
import com.quickcommerce.inventory.exception.ProductNotFoundException;
import com.quickcommerce.inventory.exception.SkuAlreadyExistsException;
import com.quickcommerce.inventory.service.ProductService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = ProductController.class,
        properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
@Import(SecurityConfig.class)
class ProductControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProductService service;
    @MockitoBean JwtDecoder jwtDecoder;

    private static final String VALID_PRODUCT = """
            {"sku":"MILK-1L","name":"Milk 1L","priceInPaise":6500,"initialStock":40}
            """;
    private static final ProductResponse MILK = new ProductResponse(1L, "MILK-1L", "Milk 1L", 6500, 40);

    @Test
    void guestCanListProductsWithoutToken() throws Exception {
        when(service.list()).thenReturn(List.of(MILK));
        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("MILK-1L"))
                .andExpect(jsonPath("$[0].available").value(40));
    }

    @Test
    void anyLoggedInUserCanListProducts() throws Exception {
        when(service.list()).thenReturn(List.of(MILK));
        mvc.perform(get("/api/v1/products").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("MILK-1L"))
                .andExpect(jsonPath("$[0].available").value(40));
    }

    @Test
    void aCustomerCannotCreateAProduct() throws Exception {
        mvc.perform(post("/api/v1/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_PRODUCT))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAdminCanCreateAProduct() throws Exception {
        when(service.create(any())).thenReturn(MILK);
        mvc.perform(post("/api/v1/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_PRODUCT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void negativeStockReturns400() throws Exception {
        mvc.perform(post("/api/v1/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"sku":"MILK-1L","name":"Milk 1L","priceInPaise":6500,"initialStock":-1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateSkuReturns409() throws Exception {
        when(service.create(any())).thenThrow(new SkuAlreadyExistsException());
        mvc.perform(post("/api/v1/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_PRODUCT))
                .andExpect(status().isConflict());
    }

    @Test
    void anAdminCanSetStock() throws Exception {
        when(service.setStock(anyLong(), anyInt())).thenReturn(new ProductResponse(1L, "MILK-1L", "Milk 1L", 6500, 10));
        mvc.perform(put("/api/v1/products/1/stock")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"quantity":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(10));
    }

    @Test
    void unknownProductReturns404() throws Exception {
        when(service.get(99L)).thenThrow(new ProductNotFoundException());
        mvc.perform(get("/api/v1/products/99").with(jwt())).andExpect(status().isNotFound());
    }
}