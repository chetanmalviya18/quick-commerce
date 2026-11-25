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
    private final StockAvailability stockAvailability;

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
        int available = stockAvailability.availableFor(product);
        stockCounter.set(id, available);
        log.info("Stock set: product id={}, on hand={}, available={}", id, quantity, available);
        return toResponse(product, available);
    }

    private int available(Product product) {
        return stockCounter.get(product.getId()).orElseGet(() -> {
            int available = stockAvailability.availableFor(product);
            stockCounter.initializeIfAbsent(product.getId(), available);
             return available;
        });
    }

    private ProductResponse toResponse(Product product, int available) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(),
                product.getPriceInPaise(), available);
    }
}
