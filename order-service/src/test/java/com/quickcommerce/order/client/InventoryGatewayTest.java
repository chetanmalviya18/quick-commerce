package com.quickcommerce.order.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.quickcommerce.order.dto.responsetDto.ProductInfo;
import com.quickcommerce.order.exception.InventoryUnavailableException;
import com.quickcommerce.order.exception.ProductNotFoundException;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InventoryGatewayTest {
    private final InventoryClient client = mock(InventoryClient.class);
    private final InventoryGateway gateway = new InventoryGateway(client);

    private Request request() {
        return Request.create(Request.HttpMethod.GET, "/api/v1/products/1", Map.of(), null, new RequestTemplate());
    }

    @Test
    void returnsTheProductWhenInventoryHasIt() {
        ProductInfo milk = new ProductInfo(5L, "MILK-1L", "Milk 1L", 6500, 40);
        when(client.getProduct(5L)).thenReturn(milk);
        assertEquals(milk, gateway.getProduct(5L));
    }

    @Test
    void aMissingProductBecomesProductNotFound() {
        when(client.getProduct(9L)).thenThrow(new FeignException.NotFound("not found", request(), null, Map.of()));
        assertThrows(ProductNotFoundException.class, () -> gateway.getProduct(9L));
    }

    @Test
    void anyOtherFailureBecomesInventoryUnavailable() {
        when(client.getProduct(5L))
                .thenThrow(new FeignException.InternalServerError("boom", request(), null, Map.of()));
        assertThrows(InventoryUnavailableException.class, () -> gateway.getProduct(5L));
    }
}