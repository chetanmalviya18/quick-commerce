package com.quickcommerce.inventory;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import com.quickcommerce.inventory.stock.StockCounter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:reservationdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.inventory.hold-minutes=10",
        "app.inventory.sweep-interval-ms=3600000"
})
@AutoConfigureMockMvc
class ReservationIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoBean StockCounter counter;
    @MockitoBean JwtDecoder jwtDecoder;

    private JwtRequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private long createProduct(String sku, int stock) throws Exception {
        MvcResult created = mvc.perform(post("/api/v1/products").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"" + sku + "\",\"name\":\"Test\",\"priceInPaise\":1000,\"initialStock\":" + stock + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String reserveJson(String orderId, long productId, int quantity) {
        return "{\"orderId\":\"" + orderId + "\",\"items\":[{\"productId\":" + productId
                + ",\"quantity\":" + quantity + "}]}";
    }

    private void reserve(String orderId, long productId, int quantity) throws Exception {
        mvc.perform(post("/api/v1/reservations").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(reserveJson(orderId, productId, quantity)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("HELD"));
    }

    private void expectAvailable(long productId, int expected) throws Exception {
        mvc.perform(get("/api/v1/products/" + productId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(expected));
    }

    @Test
    void reservingHoldsStockAndReleasingGivesItBack() throws Exception {
        when(counter.tryReserve(anyMap())).thenReturn(true);
        long id = createProduct("RES-RELEASE", 40);

        reserve("it-release", id, 2);
        expectAvailable(id, 38);

        mvc.perform(post("/api/v1/reservations/it-release/release").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"));
        expectAvailable(id, 40);
    }

    @Test
    void confirmingMakesTheReductionPermanent() throws Exception {
        when(counter.tryReserve(anyMap())).thenReturn(true);
        long id = createProduct("RES-CONFIRM", 40);

        reserve("it-confirm", id, 2);
        mvc.perform(post("/api/v1/reservations/it-confirm/confirm").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        expectAvailable(id, 38);
        mvc.perform(post("/api/v1/reservations/it-confirm/release").with(admin()))
                .andExpect(status().isConflict());
    }

    @Test
    void reservingTheSameOrderTwiceHoldsTheStockOnlyOnce() throws Exception {
        when(counter.tryReserve(anyMap())).thenReturn(true);
        long id = createProduct("RES-TWICE", 40);

        reserve("it-twice", id, 2);
        reserve("it-twice", id, 2);

        expectAvailable(id, 38);
        verify(counter, times(1)).tryReserve(anyMap());
    }

    @Test
    void notEnoughStockReturns409() throws Exception {
        when(counter.tryReserve(anyMap())).thenReturn(false);
        long id = createProduct("RES-SHORT", 1);

        mvc.perform(post("/api/v1/reservations").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(reserveJson("it-short", id, 5)))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmingAnUnknownOrderReturns404() throws Exception {
        mvc.perform(post("/api/v1/reservations/no-such-order/confirm").with(admin()))
                .andExpect(status().isNotFound());
    }
}