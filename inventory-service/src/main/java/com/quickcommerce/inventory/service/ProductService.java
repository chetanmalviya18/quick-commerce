package com.quickcommerce.inventory.service;

import com.quickcommerce.inventory.dto.requestDto.CreateProductRequest;
import com.quickcommerce.inventory.dto.responseDto.ProductResponse;
import com.quickcommerce.inventory.exception.ProductNotFoundException;
import com.quickcommerce.inventory.exception.SkuAlreadyExistsException;
import com.quickcommerce.inventory.model.Product;
import com.quickcommerce.inventory.repository.ProductRepository;
import com.quickcommerce.inventory.stock.StockCounter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final StockCounter stockCounter;

    @Transactional
    public ProductResponse create(CreateProductRequest req) {
        if (productRepository.existsBySku(req.sku())) throw new SkuAlreadyExistsException();
        Product savedProduct = productRepository.save(new Product(req.sku(), req.name(), req.priceInPaise(),
                req.initialStock()));
        stockCounter.set(savedProduct.getId(), savedProduct.getOnHand());
        log.info("Product created: id={}, sku={}", savedProduct.getId(), savedProduct.getSku());
        return toResponse(savedProduct, savedProduct.getOnHand());
    }

    @Transactional(readOnly = true)
    public ProductResponse get(long id) {
        Product product = productRepository.findById(id).orElseThrow(ProductNotFoundException::new);
        return toResponse(product, available(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        return productRepository.findAllByActiveTrue().stream()
                .map(product -> toResponse(product, available(product)))
                .toList();
    }

    // Plan 4 changes the new available figure to on-hand minus held quantity.
    @Transactional
    public ProductResponse setStock(long id, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        Product product = productRepository.findById(id).orElseThrow(ProductNotFoundException::new);
        product.changeStock(quantity);
        stockCounter.set(id, quantity);
        log.info("Stock set: product id={}, quantity={}", id, quantity);
        return toResponse(product, quantity);
    }

    private int available(Product product) {
        return stockCounter.get(product.getId()).orElseGet(() -> {
            stockCounter.initializeIfAbsent(product.getId(), product.getOnHand());
             return product.getOnHand();
        });
    }

    private ProductResponse toResponse(Product product, int available) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(),
                product.getPriceInPaise(), available);
    }
}
