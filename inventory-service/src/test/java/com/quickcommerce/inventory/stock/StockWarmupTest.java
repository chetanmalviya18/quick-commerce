package com.quickcommerce.inventory.stock;

import static org.mockito.Mockito.*;

import com.quickcommerce.inventory.model.Product;
import com.quickcommerce.inventory.repository.ProductRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

class StockWarmupTest {
    private Product product(long id, int onHand) {
        Product p = new Product("SKU-" + id, "Name", 100, onHand);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    void createsACounterForEveryProductWithoutOverwritingLiveOnes() throws Exception {
        ProductRepository products = mock(ProductRepository.class);
        StockCounter counter = mock(StockCounter.class);
        when(products.findAllByActiveTrue()).thenReturn(List.of(product(1L, 40), product(2L, 0)));

        new StockWarmup(products, counter).run(mock(ApplicationArguments.class));

        verify(counter).initializeIfAbsent(1L, 40);
        verify(counter).initializeIfAbsent(2L, 0);
        verify(counter, never()).set(anyLong(), anyInt());
    }
}