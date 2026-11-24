package com.quickcommerce.inventory;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
        "spring.datasource.url=jdbc:h2:mem:inventorydb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.inventory.hold-minutes=10",
        "app.inventory.sweep-interval-ms=3600000"
})
@AutoConfigureMockMvc
class InventoryIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoBean StockCounter counter;
    @MockitoBean JwtDecoder jwtDecoder;

    private JwtRequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private String productJson(String sku) {
        return "{\"sku\":\"" + sku + "\",\"name\":\"Test \",\"priceInPaise\":1000,\"initialStock\":40}";
    }

    @Test
    void anAdminCreatesAProductAndItAppearsInTheList() throws Exception {
        mvc.perform(post("/api/v1/products").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(productJson("INT-MILK")))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/products").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sku=='INT-MILK')]").exists());
    }

    @Test
    void creatingTheSameSkuTwiceReturns409() throws Exception {
        mvc.perform(post("/api/v1/products").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(productJson("INT-DUP")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/products").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(productJson("INT-DUP")))
                .andExpect(status().isConflict());
    }

    @Test
    void aCustomerCannotCreateAProduct() throws Exception {
        mvc.perform(post("/api/v1/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON).content(productJson("INT-NOPE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void settingStockUpdatesTheProductAndTheCounter() throws Exception {
        MvcResult created = mvc.perform(post("/api/v1/products").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(productJson("INT-STOCK")))
                .andExpect(status().isCreated())
                .andReturn();
        long id = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(put("/api/v1/products/" + id + "/stock").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(5));

        verify(counter).set(id, 5);
    }
}