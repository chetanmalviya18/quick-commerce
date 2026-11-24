package com.quickcommerce.inventory.stock;

import com.quickcommerce.inventory.model.Product;
import com.quickcommerce.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class StockWarmup implements ApplicationRunner {
    private final ProductRepository productRepository;
    private final StockCounter stockCounter;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Product> all = productRepository.findAllByActiveTrue();
        all.forEach(product -> stockCounter.initializeIfAbsent(product.getId(), product.getOnHand()));
    }
}
