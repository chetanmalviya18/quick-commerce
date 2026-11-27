package com.quickcommerce.order.client;

import com.quickcommerce.order.dto.responsetDto.ProductInfo;
import com.quickcommerce.order.exception.InventoryUnavailableException;
import com.quickcommerce.order.exception.ProductNotFoundException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryGateway {
    private final InventoryClient client;

    public ProductInfo getProduct(long id) {
        try {
            return client.getProduct(id);
        } catch (FeignException.NotFound e) {
            log.info("Product not found");
            throw new ProductNotFoundException();
        } catch (FeignException e) {
            log.error("Inventory call failed: status={}", e.status());
            throw new InventoryUnavailableException();
        }
    }
}
