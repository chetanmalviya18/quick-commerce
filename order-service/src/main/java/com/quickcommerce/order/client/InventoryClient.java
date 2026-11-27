package com.quickcommerce.order.client;

import com.quickcommerce.order.dto.responsetDto.ProductInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "inventory-service", configuration = ServiceAuthFeignConfig.class)
public interface InventoryClient {

    @GetMapping("/api/v1/products/{id}")
    ProductInfo getProduct(@PathVariable("id") long id);
}
