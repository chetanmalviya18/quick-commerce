package com.quickcommerce.inventory.service;

import com.quickcommerce.inventory.dto.requestDto.CreateProductRequest;
import com.quickcommerce.inventory.dto.responseDto.ProductResponse;
import com.quickcommerce.inventory.exception.ProductNotFoundException;
import com.quickcommerce.inventory.exception.SkuAlreadyExistsException;
import com.quickcommerce.inventory.model.Product;
import com.quickcommerce.inventory.repository.ProductRepository;
import com.quickcommerce.inventory.stock.StockCounter;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ProductServiceTest {
    private final ProductRepository products = mock(ProductRepository.class);
    private final StockCounter counter = mock(StockCounter.class);
    private final StockAvailability availability = mock(StockAvailability.class);
    private final ProductService service = new ProductService(products, counter, availability);

    private Product product(long id, String sku, int onHand) {
        Product p = new Product(sku, "Name " + sku, 5000, onHand);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    void createSavesTheProductAndSeedsTheCounter() {
        when(products.existsBySku("MILK-1L")).thenReturn(false);
        when(products.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 3L);
            return p;
        });

        ProductResponse res = service.create(new CreateProductRequest("MILK-1L", "Milk 1L", 6500, 40));

        verify(counter).set(3L, 40);
        assertEquals(3L, res.id());
        assertEquals(40, res.available());
    }

    @Test
    void createRejectsADuplicateSku() {
        when(products.existsBySku("MILK-1L")).thenReturn(true);
        assertThrows(SkuAlreadyExistsException.class,
                () -> service.create(new CreateProductRequest("MILK-1L", "Milk 1L", 6500, 40)));
        verify(products, never()).save(any());
    }

    @Test
    void getReadsAvailabilityFromRedis() {
        when(products.findById(3L)).thenReturn(Optional.of(product(3L, "MILK-1L", 40)));
        when(counter.get(3L)).thenReturn(Optional.of(25));
        assertEquals(25, service.get(3L).available());
    }

    @Test
    void getRebuildsAMissingCounterFromTheDatabase() {
        Product p = product(3L, "MILK-1L", 40);
        when(products.findById(3L)).thenReturn(Optional.of(p));
        when(counter.get(3L)).thenReturn(Optional.empty());
        when(availability.availableFor(p)).thenReturn(40);

        assertEquals(40, service.get(3L).available());
        verify(counter).initializeIfAbsent(3L, 40);
    }

    @Test
    void getUnknownProductFails() {
        when(products.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> service.get(99L));
    }

    @Test
    void listReturnsEveryProductWithItsAvailability() {
        when(products.findAllByActiveTrue()).thenReturn(List.of(product(1L, "A", 5), product(2L, "B", 7)));
        when(counter.get(1L)).thenReturn(Optional.of(5));
        when(counter.get(2L)).thenReturn(Optional.of(3));

        List<ProductResponse> result = service.list();

        assertEquals(2, result.size());
        assertEquals(3, result.get(1).available());
    }

    @Test
    void setStockUpdatesTheDatabaseAndTheCounter() {
        Product p = product(3L, "MILK-1L", 40);
        when(products.findById(3L)).thenReturn(Optional.of(p));
        when(availability.availableFor(p)).thenReturn(8);

        ProductResponse res = service.setStock(3L, 10);

        assertEquals(10, p.getOnHand());
        verify(counter).set(3L, 8);
        assertEquals(8, res.available());
    }

    @Test
    void setStockRejectsNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () -> service.setStock(3L, -1));
        verifyNoInteractions(products, counter, availability);
    }
}
