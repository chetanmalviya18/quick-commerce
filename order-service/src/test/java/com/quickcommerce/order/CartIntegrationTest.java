package com.quickcommerce.order;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.quickcommerce.order.cart.CartStore;
import com.quickcommerce.order.client.InventoryGateway;
import com.quickcommerce.order.dto.responsetDto.ProductInfo;
import com.quickcommerce.order.exception.ProductNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:orderdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "app.service-account.email=svc@internal.local",
        "app.service-account.password=secret",
        "app.cart.max-quantity-per-item=20",
        "app.cart.max-items=50",
        "app.cart.ttl-days=7",
        "app.order.delivery-fee-paise=2500",
        "app.order.sweep-interval-ms=3600000",
        "app.order.stale-created-minutes=2"
})
@AutoConfigureMockMvc
class CartIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoBean InventoryGateway inventory;
    @MockitoBean JwtDecoder jwtDecoder;

    @TestConfiguration
    static class InMemoryCart {
        @Bean
        @Primary
        CartStore cartStore() {
            return new CartStore() {
                private final Map<Long, Map<Long, Integer>> carts = new ConcurrentHashMap<>();

                @Override
                public Map<Long, Integer> get(long userId) {
                    return new LinkedHashMap<>(carts.getOrDefault(userId, Map.of()));
                }

                @Override
                public void setQuantity(long userId, long productId, int quantity) {
                    Map<Long, Integer> cart = carts.computeIfAbsent(userId, id -> new LinkedHashMap<>());
                    if (quantity == 0) {
                        cart.remove(productId);
                    } else {
                        cart.put(productId, quantity);
                    }
                }

                @Override
                public void clear(long userId) {
                    carts.remove(userId);
                }
            };
        }
    }

    private JwtRequestPostProcessor customer(String userId) {
        return jwt().jwt(j -> j.subject(userId)).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private static final String ITEM = "{\"productId\":10,\"quantity\":3}";

    @Test
    void aCustomerAddsAProductAndSeesItInTheCart() throws Exception {
        when(inventory.getProduct(10L)).thenReturn(new ProductInfo(10L, "MILK-1L", "Milk 1L", 6500, 40));

        mvc.perform(put("/api/v1/cart/items").with(customer("101"))
                        .contentType(MediaType.APPLICATION_JSON).content(ITEM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3));

        mvc.perform(get("/api/v1/cart").with(customer("101")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(10));
    }

    @Test
    void settingTheQuantityToZeroRemovesTheProduct() throws Exception {
        when(inventory.getProduct(10L)).thenReturn(new ProductInfo(10L, "MILK-1L", "Milk 1L", 6500, 40));
        mvc.perform(put("/api/v1/cart/items").with(customer("102"))
                .contentType(MediaType.APPLICATION_JSON).content(ITEM)).andExpect(status().isOk());

        mvc.perform(put("/api/v1/cart/items").with(customer("102"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":10,\"quantity\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void anUnknownProductReturns404() throws Exception {
        when(inventory.getProduct(99L)).thenThrow(new ProductNotFoundException());

        mvc.perform(put("/api/v1/cart/items").with(customer("103"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":99,\"quantity\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anAdminCannotUseTheCart() throws Exception {
        mvc.perform(get("/api/v1/cart").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }
}